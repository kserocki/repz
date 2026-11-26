package com.repz.app.core.domain.exercise

import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.localizedName

private val QUERY_TOKEN_SEPARATOR = Regex("\\s+")
private val NAME_WORD_SEPARATOR = Regex("[\\s-]+")

/**
 * Filters by [query] and ranks the matches; a blank query keeps every exercise, ordered by
 * localized name. An exercise matches when every whitespace-separated, lowercased token of [query]
 * is a substring of its localized name or of its English name (no diacritic folding). Ranked 0 when
 * the localized name starts with the whole normalised query, 1 when any of its whitespace/hyphen
 * separated words starts with the query's first token, 2 otherwise; ties break by localized name,
 * then by id.
 */
fun List<Exercise>.searchedBy(
    query: String,
    languageTag: String,
): List<Exercise> {
    val tokens =
        query
            .trim()
            .lowercase()
            .split(QUERY_TOKEN_SEPARATOR)
            .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        return sortedWith(compareBy({ exercise -> exercise.localizedName(languageTag).lowercase() }, { exercise -> exercise.sortId() }))
    }

    val normalizedQuery = tokens.joinToString(separator = " ")
    val firstToken = tokens.first()
    return filter { exercise -> tokens.all { token -> exercise.matchesToken(token, languageTag) } }
        .sortedWith(
            compareBy(
                { exercise -> exercise.searchRank(normalizedQuery, firstToken, languageTag) },
                { exercise -> exercise.localizedName(languageTag).lowercase() },
                { exercise -> exercise.sortId() },
            ),
        )
}

private fun Exercise.matchesToken(
    token: String,
    languageTag: String,
): Boolean = localizedName(languageTag).lowercase().contains(token) || name.lowercase().contains(token)

private fun Exercise.searchRank(
    normalizedQuery: String,
    firstToken: String,
    languageTag: String,
): Int {
    val localized = localizedName(languageTag).lowercase()
    return when {
        localized.startsWith(normalizedQuery) -> 0
        localized.split(NAME_WORD_SEPARATOR).any { it.startsWith(firstToken) } -> 1
        else -> 2
    }
}

private fun Exercise.sortId(): String = id.value.toString()
