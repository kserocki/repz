package com.repz.buildlogic.i18n

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The locale tag an unqualified `values` folder maps to (ADR-0014, ADR-0017). */
const val DEFAULT_APP_LOCALE_TAG = "en"

/**
 * One app language, discovered from a `values[-qualifiers]` Compose resources folder.
 *
 * @property tag the tag used everywhere else in the build (`en`, `pl`, `pt-BR`).
 * @property valuesDirectoryName the folder it was read from (`values`, `values-pl`, `values-pt-rBR`).
 * @property appName the `app_name` string declared in that folder.
 */
data class AppLocale(
    val tag: String,
    val valuesDirectoryName: String,
    val appName: String,
)

private const val APP_NAME_RESOURCE = "app_name"
private const val VALUES_PREFIX = "values"
private val LANGUAGE_QUALIFIER = Regex("^[a-z]{2,3}$")
private val REGION_QUALIFIER = Regex("^r([A-Z]{2})$")
private val IGNORED_QUALIFIERS =
    setOf(
        "light",
        "dark",
        "ldpi",
        "mdpi",
        "hdpi",
        "xhdpi",
        "xxhdpi",
        "xxxhdpi",
    )

/**
 * Reads every `values[-qualifiers]/strings.xml` directly under [composeResourcesDirectory] and
 * turns it into the [AppLocale] Repz treats as a supported language (ADR-0017).
 *
 * Only a directory whose qualifiers are *exactly* a language, optionally followed by a region
 * (`values`, `values-pl`, `values-pt-rBR`) declares a language. A directory that also carries a
 * theme or density qualifier (`values-dark`, `values-pl-dark`, …) is a themed/density *variant* of
 * an already-declared language, never a declaration on its own, so it is skipped entirely: it
 * never creates a new locale and never collides with one — including when its base folder
 * (`values-pl` for `values-pl-dark`) is missing: the language is then silently absent from the
 * result, the same as if no folder for it existed at all. Any other qualifier is an error, because
 * Repz has no rule for what it means for the supported-language list.
 *
 * The unqualified `values` folder maps to [DEFAULT_APP_LOCALE_TAG]. The result is sorted with the
 * default locale first, then the rest alphabetically by tag.
 */
fun readAppLocales(composeResourcesDirectory: File): List<AppLocale> {
    val directories =
        composeResourcesDirectory
            .listFiles { file -> file.isDirectory && isValuesDirectoryName(file.name) }
            .orEmpty()
            .sortedBy { it.name }

    val locales = mutableListOf<AppLocale>()
    for (directory in directories) {
        val tag = resolveLocaleTag(directory) ?: continue

        locales.find { it.tag == tag }?.let { existing ->
            throw GradleException(
                "Duplicate locale tag '$tag': both ${existing.valuesDirectoryName} and " +
                    "${directory.name} resolve to it.",
            )
        }

        val appName =
            readAppName(directory)
                ?: throw GradleException(
                    "Missing <string name=\"$APP_NAME_RESOURCE\"> in ${directory.name}/strings.xml",
                )
        locales += AppLocale(tag = tag, valuesDirectoryName = directory.name, appName = appName)
    }

    return locales.sortedWith(compareBy({ it.tag != DEFAULT_APP_LOCALE_TAG }, { it.tag }))
}

/** `mobile/app/shared/src/commonMain/composeResources`, the canonical language folders (ADR-0017). */
val Project.appLocalesSourceDirectory: Directory
    get() = rootProject.layout.projectDirectory.dir("app/shared/src/commonMain/composeResources")

private fun isValuesDirectoryName(name: String): Boolean = name == VALUES_PREFIX || name.startsWith("$VALUES_PREFIX-")

/** `null` means "not a language declaration" (a theme/density variant): skip, not an error. */
private fun resolveLocaleTag(directory: File): String? {
    if (directory.name == VALUES_PREFIX) return DEFAULT_APP_LOCALE_TAG

    val qualifiers = directory.name.removePrefix("$VALUES_PREFIX-").split("-")
    var language: String? = null
    var region: String? = null
    var hasIgnoredQualifier = false

    for (qualifier in qualifiers) {
        val regionMatch = REGION_QUALIFIER.matchEntire(qualifier)
        when {
            qualifier in IGNORED_QUALIFIERS -> hasIgnoredQualifier = true
            language == null && LANGUAGE_QUALIFIER.matches(qualifier) -> language = qualifier
            region == null && regionMatch != null -> region = regionMatch.groupValues[1]
            else -> throw unsupportedQualifier(qualifier, directory)
        }
    }

    return when {
        hasIgnoredQualifier -> null
        language != null && region != null -> "$language-$region"
        language != null -> language
        else -> throw unsupportedQualifier(directory.name.removePrefix("$VALUES_PREFIX-"), directory)
    }
}

private fun unsupportedQualifier(
    qualifier: String,
    directory: File,
): GradleException =
    GradleException(
        "Unsupported resource qualifier '$qualifier' in ${directory.name}; " +
            "supported: language, rRegion, theme, density",
    )

private fun readAppName(directory: File): String? {
    val stringsFile = File(directory, "strings.xml")
    if (!stringsFile.isFile) return null

    val document =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(stringsFile)

    val stringElements = document.getElementsByTagName("string")
    return (0 until stringElements.length)
        .asSequence()
        .map { index -> stringElements.item(index) }
        .firstOrNull { element -> element.attributes?.getNamedItem("name")?.nodeValue == APP_NAME_RESOURCE }
        ?.textContent
        ?.trim()
}
