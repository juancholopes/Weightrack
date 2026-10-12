package com.juancholopes.weightrack.feature.catalog.domain

import java.text.Normalizer
import java.util.Locale

private val diacriticalMarks = Regex("\\p{Mn}+")

/**
 * S10-R33: the search key of a text. Normalized as in S10-R04, lowercased with `Locale.ROOT`, decomposed
 * to NFD and without diacritical marks (Unicode category `Mn`), so "ñ" counts as "n". Unlike the
 * uniqueness key of S10-R05, accents do not count here.
 */
fun exerciseSearchKey(text: String): String =
    Normalizer.normalize(normalizeExerciseName(text).lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(diacriticalMarks, "")

/** S10-R33: [name] matches when the key of [query] appears inside its key. A blank query matches all. */
fun matchesExerciseSearch(name: String, query: String): Boolean =
    exerciseSearchKey(name).contains(exerciseSearchKey(query))
