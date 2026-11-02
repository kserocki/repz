package com.repz.app.core.domain.model

// ADR-0021: no other language is ever named in code; this is the one documented fallback.
private const val FALLBACK_LANGUAGE_TAG = "en"

/**
 * The catalog name for [languageTag] (a BCP 47 tag): an exact tag match (case-insensitive), then a
 * translation sharing the same primary language subtag (`pt-PT` falls back to a `pt-BR`
 * translation), then `en`, then [Exercise.name] (always present, and the only option for a custom
 * exercise, which has no translations).
 */
fun Exercise.localizedName(languageTag: String): String = translationsByLocalePriority(languageTag).firstOrNull()?.name ?: name

/**
 * Same fallback chain as [localizedName], ending in `null`. Unlike the name, a matched
 * translation's instructions can themselves be `null` (ADR-0021 makes instructions optional per
 * locale), so this keeps walking the chain until one has instructions.
 */
fun Exercise.localizedInstructions(languageTag: String): String? =
    translationsByLocalePriority(languageTag).firstNotNullOfOrNull { it.instructions }

private fun Exercise.translationsByLocalePriority(languageTag: String): List<ExerciseTranslation> {
    val primaryLanguage = languageTag.substringBefore('-')
    val exactTag = translations.filter { it.languageTag.equals(languageTag, ignoreCase = true) }
    val samePrimaryLanguage = translations.filter { it.languageTag.substringBefore('-').equals(primaryLanguage, ignoreCase = true) }
    val english = translations.filter { it.languageTag.equals(FALLBACK_LANGUAGE_TAG, ignoreCase = true) }
    return exactTag + samePrimaryLanguage + english
}
