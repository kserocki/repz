package com.repz.buildlogic.android

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.repz.buildlogic.i18n.DEFAULT_APP_LOCALE_TAG
import com.repz.buildlogic.i18n.appLocalesSourceDirectory
import com.repz.buildlogic.i18n.readAppLocales
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Generates the Android half of the canonical language list (ADR-0017): one
 * `values[-<qualifier>]/strings.xml` per locale read from [sourceDirectory], each declaring
 * `app_name`. AGP's `generateLocaleConfig` (`androidResources { generateLocaleConfig = true }`)
 * reads whatever language-qualified `res/values-*` folders the variant sees and writes
 * `android:localeConfig` + `locale_config.xml` from them; [registerGenerateAndroidAppNameResources]
 * registers this task's output directory as one of them.
 */
@CacheableTask
abstract class GenerateAndroidAppNameResourcesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        readAppLocales(sourceDirectory.get().asFile).forEach { locale ->
            val valuesDirectory = File(outputDir, androidValuesDirectoryName(locale.tag)).apply { mkdirs() }
            File(valuesDirectory, "strings.xml").writeText(stringsXml(locale.appName))
        }
    }

    private fun stringsXml(appName: String): String =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <string name=\"app_name\">${escapeAndroidString(appName)}</string>\n" +
            "</resources>\n"
}

/**
 * Registers [GenerateAndroidAppNameResourcesTask] reading this project's canonical language
 * folders and adds its output as a generated `res` directory on every variant, so AGP's
 * `generateLocaleConfig` picks it up the same way it would a hand-written `res/values-*` folder.
 */
fun Project.registerGenerateAndroidAppNameResources() {
    val generateTask =
        tasks.register<GenerateAndroidAppNameResourcesTask>("generateAndroidAppNameResources") {
            sourceDirectory.set(appLocalesSourceDirectory)
            outputDirectory.set(layout.buildDirectory.dir("generated/androidAppNameResources"))
        }

    extensions.getByType<ApplicationAndroidComponentsExtension>().onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            generateTask,
            GenerateAndroidAppNameResourcesTask::outputDirectory,
        )
    }
}

/** `en` -> `values`, `pl` -> `values-pl`, `pt-BR` -> `values-pt-rBR` (Android's region qualifier form). */
internal fun androidValuesDirectoryName(tag: String): String {
    if (tag == DEFAULT_APP_LOCALE_TAG) return "values"
    val (language, region) = tag.split("-", limit = 2).let { it[0] to it.getOrNull(1) }
    return if (region != null) "values-$language-r$region" else "values-$language"
}

/** Escapes the characters Android's string-resource grammar is sensitive to: `'`, `"`, `@`, `?`, `&`, `<`. */
internal fun escapeAndroidString(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("@", "\\@")
        .replace("?", "\\?")
