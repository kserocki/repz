package com.repz.buildlogic.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Generates the iOS half of the canonical language list (ADR-0017): one
 * `<tag>.lproj/InfoPlist.strings` per [AppLocale] read from [sourceDirectory], plus an
 * `app-localizations.txt` index (one tag per line, default first).
 *
 * Consumed at Xcode build time by `mobile/iosApp/scripts/apply-app-localizations.sh`, which copies
 * every generated `.lproj` folder into the built app and sets `CFBundleLocalizations` from the
 * index.
 */
@CacheableTask
abstract class GenerateIosAppLocalizationsTask : DefaultTask() {

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

        val locales = readAppLocales(sourceDirectory.get().asFile)

        locales.forEach { locale ->
            val lprojDirectory = File(outputDir, "${locale.tag}.lproj").apply { mkdirs() }
            File(lprojDirectory, "InfoPlist.strings").writeText(infoPlistStrings(locale.appName))
        }

        File(outputDir, "app-localizations.txt")
            .writeText(locales.joinToString(separator = "\n", postfix = "\n") { it.tag })
    }

    private fun infoPlistStrings(appName: String): String {
        val escapedAppName = appName.replace("\\", "\\\\").replace("\"", "\\\"")
        return buildString {
            appendLine("\"CFBundleDisplayName\" = \"$escapedAppName\";")
            appendLine("\"CFBundleName\" = \"$escapedAppName\";")
        }
    }
}
