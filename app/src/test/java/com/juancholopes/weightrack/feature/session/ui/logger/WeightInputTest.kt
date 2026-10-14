package com.juancholopes.weightrack.feature.session.ui.logger

import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightInputTest {

    private fun valid(value: Int) = ParsedValue.Valid(value)

    private fun rejected(error: ExerciseSetError) = ParsedValue.Invalid(TypedValueError.Rejected(error))

    // SF02-A09 / SF02-R15, S42-A15: "62,5" and "62.5" set 62 500 g
    @Test
    fun parseWeightInput_acceptsACommaOrAPoint() {
        assertEquals(valid(62_500), parseWeightInput("62,5"))
        assertEquals(valid(62_500), parseWeightInput("62.5"))
        assertEquals(valid(62_250), parseWeightInput("62,25"))
        assertEquals(valid(60_000), parseWeightInput("60"))
        assertEquals(valid(60_000), parseWeightInput("60,0"))
        assertEquals(valid(500), parseWeightInput(",5"))
        assertEquals(valid(62_500), parseWeightInput("  62,5 "))
    }

    // SF02-A09 / S40-A24, S40-R08: 0 and 1000 kg are accepted
    @Test
    fun parseWeightInput_acceptsTheLimits() {
        assertEquals(valid(0), parseWeightInput("0"))
        assertEquals(valid(0), parseWeightInput("0,00"))
        assertEquals(valid(1_000_000), parseWeightInput("1000"))
        assertEquals(valid(1_000_000), parseWeightInput("1000,00"))
    }

    // SF02-A09 / SF02-R15, S42-A15: "62,3" is not a multiple of 0,25 kg and is never rounded
    @Test
    fun parseWeightInput_rejectsAWeightThatIsNotAMultipleOfTheStep() {
        assertEquals(rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), parseWeightInput("62,3"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), parseWeightInput("62.1"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), parseWeightInput("0,1"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), parseWeightInput("62,2501"))
    }

    // SF02-A09 / SF02-R15: "1000,25" is above 1000 kg
    @Test
    fun parseWeightInput_rejectsAWeightAbove1000Kg() {
        assertEquals(rejected(ExerciseSetError.WEIGHT_TOO_HIGH), parseWeightInput("1000,25"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_TOO_HIGH), parseWeightInput("1001"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_TOO_HIGH), parseWeightInput("99999999999999"))
    }

    // SF02-A09 / SF02-R15: a negative weight is rejected
    @Test
    fun parseWeightInput_rejectsANegativeWeight() {
        assertEquals(rejected(ExerciseSetError.WEIGHT_NEGATIVE), parseWeightInput("-5"))
        assertEquals(rejected(ExerciseSetError.WEIGHT_NEGATIVE), parseWeightInput("-0,25"))
    }

    // SF02-A09 / SF02-R15, Spec 42 "Escribe un número, por ejemplo 62,5.": text that is not a number
    @Test
    fun parseWeightInput_rejectsTextThatIsNotANumber() {
        val notANumber = ParsedValue.Invalid(TypedValueError.NotANumber)
        for (text in listOf("", "   ", "abc", "62,5 kg", "6,2,5", "62..5", "-", ",", "1e3", "62 5")) {
            assertEquals("\"$text\"", notANumber, parseWeightInput(text))
        }
    }

    // SF02-A09 / SF02-R15, S42-R20: the typing dialog starts with the weight as it is shown, with a comma
    @Test
    fun weightInputText_isTheWeightWithACommaAndNoUnit() {
        assertEquals("62,5", weightInputText(62_500))
        assertEquals("60", weightInputText(60_000))
        assertEquals("", weightInputText(null))
        assertEquals(valid(62_500), parseWeightInput(weightInputText(62_500)))
    }

    // SF02-A09 / SF02-R15: the repetitions are whole numbers from 1 to 999
    @Test
    fun parseRepsInput_acceptsWholeNumbersFrom1To999() {
        assertEquals(valid(10), parseRepsInput("10"))
        assertEquals(valid(1), parseRepsInput("1"))
        assertEquals(valid(999), parseRepsInput(" 999 "))
    }

    // SF02-A09 / S40-A24: 0 and 1000 repetitions cannot be saved
    @Test
    fun parseRepsInput_rejectsOutOfRangeValues() {
        assertEquals(rejected(ExerciseSetError.REPS_NOT_POSITIVE), parseRepsInput("0"))
        assertEquals(rejected(ExerciseSetError.REPS_NOT_POSITIVE), parseRepsInput("-3"))
        assertEquals(rejected(ExerciseSetError.REPS_TOO_HIGH), parseRepsInput("1000"))
        assertEquals(rejected(ExerciseSetError.REPS_TOO_HIGH), parseRepsInput("99999999999999"))
    }

    // SF02-A09 / SF02-R15: a decimal or text is not a number of repetitions, and nothing is rounded
    @Test
    fun parseRepsInput_rejectsDecimalsAndText() {
        val notANumber = ParsedValue.Invalid(TypedValueError.NotANumber)
        for (text in listOf("", "abc", "10,5", "10.5", "1 0", "-")) {
            assertEquals("\"$text\"", notANumber, parseRepsInput(text))
        }
    }

    // SF02-R15: the typing dialog of the repetitions starts with the number
    @Test
    fun repsInputText_isTheNumber_orEmptyWhenNotFixed() {
        assertEquals("10", repsInputText(10))
        assertEquals("", repsInputText(null))
    }
}
