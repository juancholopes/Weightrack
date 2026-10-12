package com.juancholopes.weightrack.feature.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchTest {

    // SF01-A01 / SF01-R12, S10-A23 / S10-R33
    @Test
    fun matchesExerciseSearch_ignoresCaseSpacesAndAccents() {
        assertTrue(matchesExerciseSearch("Press de banca", "PRESS"))
        assertTrue(matchesExerciseSearch("Press de banca", "press  de"))
        assertTrue(matchesExerciseSearch("Press de banca", "prés"))
        assertTrue(matchesExerciseSearch("Elevación lateral", "elevacion"))
    }

    // SF01-A01 / SF01-R12, S10-A23 / S10-R33: "ñ" counts as "n"
    @Test
    fun matchesExerciseSearch_treatsEnyeAsN() {
        assertTrue(matchesExerciseSearch("Remo de peña", "pena"))
        assertTrue(matchesExerciseSearch("Remo de peña", "peña"))
        assertTrue(matchesExerciseSearch("Remo de pena", "peña"))
    }

    // SF01-A01 / SF01-R12, S10-A23 / S10-R33
    @Test
    fun matchesExerciseSearch_withEmptyOrBlankQuery_matchesEverything() {
        assertTrue(matchesExerciseSearch("Press de banca", ""))
        assertTrue(matchesExerciseSearch("Remo de peña", "   "))
    }

    // SF01-R12, S10-R33
    @Test
    fun matchesExerciseSearch_withTextNotInTheName_doesNotMatch() {
        assertFalse(matchesExerciseSearch("Press de banca", "sentadilla"))
        assertFalse(matchesExerciseSearch("Press de banca", "banca press"))
    }

    // SF01-R12, S10-R33: normalized, lowercase, without diacritics
    @Test
    fun exerciseSearchKey_normalizesLowercasesAndStripsDiacritics() {
        assertEquals("elevacion lateral", exerciseSearchKey("  Elevación   LATERAL "))
        assertEquals("remo de pena", exerciseSearchKey("Remo de PEÑA"))
        assertEquals("", exerciseSearchKey("   "))
    }
}
