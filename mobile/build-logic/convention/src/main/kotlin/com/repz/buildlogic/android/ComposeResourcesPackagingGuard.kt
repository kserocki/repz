package com.repz.buildlogic.android

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File

private const val COMPOSE_RESOURCES_SOURCE_PATH = "src/commonMain/composeResources"
private const val VERIFY_TASK_NAME = "verifyComposeResourcesPackaged"

/**
 * Regression guard for the packaging gap `configureAndroidLibraryTarget` fixes: AGP defaults
 * `androidResources.enable` to false for KMP Android library targets, which silently drops every
 * Compose resource (fonts, strings, files) from the module's AAR. If this module declares
 * `commonMain/composeResources`, registers [VerifyComposeResourcesPackagedTask] reading the
 * variant's merged [SingleArtifact.ASSETS] directory (the same public artifact `MergeSourceSetFolders`
 * publishes its output to) and fails `check` if any declared file is missing from it. A module
 * without `commonMain/composeResources` gets no task at all, so this stays cheap project-wide.
 */
internal fun Project.registerComposeResourcesPackagingGuard() {
    val composeResourcesDirectory = layout.projectDirectory.dir(COMPOSE_RESOURCES_SOURCE_PATH).asFile
    if (!composeResourcesDirectory.walkTopDown().any { it.isFile }) return

    val guardTask =
        tasks.register<VerifyComposeResourcesPackagedTask>(VERIFY_TASK_NAME) {
            group = "verification"
            description =
                "Fails if this module's commonMain/composeResources files are missing from the " +
                "variant's merged Android assets."
            composeResourcesSource.from(composeResourcesDirectory)
        }

    extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>().onVariants { variant ->
        guardTask.configure { mergedAndroidAssets.set(variant.artifacts.get(SingleArtifact.ASSETS)) }
    }

    tasks.named("check") { dependsOn(guardTask) }
}

internal abstract class VerifyComposeResourcesPackagedTask : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResourcesSource: ConfigurableFileCollection

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mergedAndroidAssets: DirectoryProperty

    @TaskAction
    fun verify() {
        val sourceDirectory = composeResourcesSource.singleFile
        if (!mergedAndroidAssets.isPresent) {
            throw GradleException(
                "No merged Android assets were published for this variant (SingleArtifact.ASSETS " +
                    "has no value), so this module's commonMain/composeResources ($sourceDirectory) " +
                    "cannot have been packaged. This usually means `androidResources.enable` " +
                    "regressed to false for this module's KMP Android library target (see " +
                    "AndroidLibraryTarget.kt).",
            )
        }
        val mergedDirectory = mergedAndroidAssets.get().asFile
        val missing = findMissingComposeResources(sourceDirectory, listOf(mergedDirectory))
        if (missing.isNotEmpty()) {
            throw GradleException(
                "composeResources files declared in $sourceDirectory are missing from the merged " +
                    "Android assets at $mergedDirectory: $missing. This usually means " +
                    "`androidResources.enable` regressed to false for this module's KMP Android " +
                    "library target (see AndroidLibraryTarget.kt).",
            )
        }
    }
}

/**
 * Every file under [composeResourcesDirectory], by its path relative to that directory (e.g.
 * `font/archivo_variable.ttf`), that is not packaged anywhere under [mergedAssetRoots].
 *
 * Compose Multiplatform copies raw resource categories (`font/`, `files/`, `drawable/`, ...)
 * byte-for-byte, nested one level deeper under the module's generated-resources package name, so
 * those are matched by same-suffix path. `values[-qualifier]/` directories are compiled from XML
 * (e.g. `strings.xml`) into a binary `.cvr` file with an unrelated name, so for those only the
 * qualifier directory itself is required to exist and be non-empty.
 */
internal fun findMissingComposeResources(
    composeResourcesDirectory: File,
    mergedAssetRoots: Collection<File>,
): List<String> {
    val mergedFiles = mergedAssetRoots.flatMap { root -> root.walkTopDown().filter { it.isFile }.toList() }
    return composeResourcesDirectory
        .walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(composeResourcesDirectory).path }
        .filterNot { relativePath -> isPackaged(relativePath, mergedFiles) }
        .toList()
}

private fun isPackaged(
    relativePath: String,
    mergedFiles: List<File>,
): Boolean {
    val topLevelDirectory = relativePath.substringBefore(File.separatorChar)
    return if (topLevelDirectory.startsWith("values")) {
        mergedFiles.any { it.path.replace(File.separatorChar, '/').contains("/$topLevelDirectory/") }
    } else {
        mergedFiles.any { it.path.endsWith(relativePath) }
    }
}
