package com.juancholopes.weightrack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class FormatsTest {

    /** The same names `strings.xml` provides, so the test does not depend on the language of the machine (S41-R14). */
    private val names = DateNames(
        weekdaysShort = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom"),
        weekdaysLong = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"),
        monthsShort = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"),
        monthsLong = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        ),
        today = "Hoy",
        yesterday = "Ayer",
    )

    /** Sunday. */
    private val today = LocalDate.of(2026, 10, 11)

    // SF02-A20 / SF02-R20, S42-A25: decimal comma, no trailing zeros, no thousands separator
    @Test
    fun formatWeight_usesACommaAndNoSuperfluousZeros() {
        assertEquals("62,5 kg", formatWeight(62_500))
        assertEquals("60 kg", formatWeight(60_000))
        assertEquals("62,25 kg", formatWeight(62_250))
        assertEquals("1000 kg", formatWeight(1_000_000))
        assertEquals("0 kg", formatWeight(0))
        assertEquals("0,25 kg", formatWeight(250))
        assertEquals("0,75 kg", formatWeight(750))
        assertEquals("1,25 kg", formatWeight(1_250))
    }

    // SF02-A20 / SF02-R20: the number alone is what the typing dialog starts with
    @Test
    fun formatKilograms_isTheWeightWithoutTheUnit() {
        assertEquals("62,5", formatKilograms(62_500))
        assertEquals("60", formatKilograms(60_000))
        assertEquals("0", formatKilograms(0))
    }

    // SF02-A20 / SF02-R20: "62,5 kilogramos" and the singular "1 kilogramo" for TalkBack
    @Test
    fun spokenWeight_readsTheUnitInFull_andUsesTheSingularForOne() {
        assertEquals("62,5 kilogramos", spokenWeight(62_500))
        assertEquals("60 kilogramos", spokenWeight(60_000))
        assertEquals("1 kilogramo", spokenWeight(1_000))
        assertEquals("0 kilogramos", spokenWeight(0))
        assertEquals("1,25 kilogramos", spokenWeight(1_250))
    }

    // SF02-A20 / SF02-R20
    @Test
    fun formatReps_isTheNumber_andSpokenRepsReadTheUnit() {
        assertEquals("10", formatReps(10))
        assertEquals("10 repeticiones", spokenReps(10))
        assertEquals("1 repetición", spokenReps(1))
    }

    // SF02-A20 / SF02-R20: "60 kg × 10", shortened as "60 × 10" inside a list of the same exercise
    @Test
    fun formatSet_joinsWeightAndRepsWithTheTimesSign() {
        assertEquals("60 kg × 10", formatSet(60_000, 10))
        assertEquals("62,5 kg × 8", formatSet(62_500, 8))
        assertEquals("60 × 10", formatSet(60_000, 10, withUnit = false))
        assertEquals("62,5 × 8", formatSet(62_500, 8, withUnit = false))
    }

    // SF02-A20 / SF02-R20
    @Test
    fun spokenSet_readsWeightAndReps() {
        assertEquals("60 kilogramos, 10 repeticiones", spokenSet(60_000, 10))
        assertEquals("62,5 kilogramos, 1 repetición", spokenSet(62_500, 1))
        assertEquals("1 kilogramo, 5 repeticiones", spokenSet(1_000, 5))
    }

    // SF02-A20 / SF02-R20, Spec 42 "Formatos": the additional weight of a bodyweight exercise
    @Test
    fun formatAdditionalWeight_showsThePlusSign_orNoWeight() {
        assertEquals("+10 kg", formatAdditionalWeight(10_000))
        assertEquals("+2,5 kg", formatAdditionalWeight(2_500))
        assertEquals("Sin peso", formatAdditionalWeight(0))
        assertEquals("10 kilogramos adicionales", spokenAdditionalWeight(10_000))
        assertEquals("1 kilogramo adicional", spokenAdditionalWeight(1_000))
        assertEquals("Sin peso", spokenAdditionalWeight(0))
    }

    // SF02-A20 / SF02-R20, S42-A25: "jue 8 oct"
    @Test
    fun formatShortDate_showsWeekdayDayAndMonth() {
        assertEquals("jue 8 oct", formatShortDate(LocalDate.of(2026, 10, 8), today, names))
        assertEquals("mié 7 oct", formatShortDate(LocalDate.of(2026, 10, 7), today, names))
        assertEquals("jue 17 sept", formatShortDate(LocalDate.of(2026, 9, 17), today, names))
    }

    // SF02-A20 / SF02-R20: "Hoy" and "Ayer" for those two days
    @Test
    fun formatShortDate_saysTodayAndYesterday() {
        assertEquals("Hoy", formatShortDate(today, today, names))
        assertEquals("Ayer", formatShortDate(today.minusDays(1), today, names))
        assertEquals("sáb 10 oct", formatShortDate(today.minusDays(1), today, names, relative = false))
        assertEquals("dom 11 oct", formatShortDate(today, today, names, relative = false))
        assertEquals("Ayer", formatShortDate(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1), names))
    }

    // SF02-A20 / SF02-R20: the year only appears when it is not the current one
    @Test
    fun formatShortDate_showsTheYearOnlyWhenItIsNotTheCurrentOne() {
        assertEquals("jue 25 dic 2025", formatShortDate(LocalDate.of(2025, 12, 25), today, names))
        assertEquals("jue 8 oct 2026", formatShortDate(LocalDate.of(2026, 10, 8), LocalDate.of(2027, 1, 3), names))
        assertEquals("jue 8 oct", formatShortDate(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 12, 31), names))
    }

    // SF02-A20 / SF02-R20: "jueves 8 de octubre" for TalkBack, with the year when it is not the current one
    @Test
    fun spokenDate_readsTheFullNames() {
        assertEquals("jueves 8 de octubre", spokenDate(LocalDate.of(2026, 10, 8), today, names))
        assertEquals("domingo 11 de octubre", spokenDate(today, today, names))
        assertEquals("jueves 25 de diciembre de 2025", spokenDate(LocalDate.of(2025, 12, 25), today, names))
    }

    // SF02-R20, S41-R14: the names come as lists, so a list of the wrong size is a programming error
    @Test
    fun dateNames_withAWrongNumberOfNames_isRejected() {
        try {
            names.copy(weekdaysShort = listOf("lun"))
            fail("Expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // the seven weekdays and the twelve months are all needed
        }
    }
}
