package com.juancholopes.weightrack.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import com.juancholopes.weightrack.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class RobotoFlexTest {

    private fun Font.axes(): Map<String, Float> =
        (this as ResourceFont).variationSettings.settings.associate { it.axisName to it.toVariationValue(null) }

    // S41-A07 / S41-R05, S41-R07
    @Test
    fun robotoFlex_default_usesBundledResourceAndFontDefaults() {
        val font = robotoFlex() as ResourceFont

        assertEquals(R.font.roboto_flex, font.resId)
        assertEquals(FontWeight(400), font.weight)
        assertEquals(mapOf("wght" to 400f, "wdth" to 100f, "GRAD" to 0f), font.axes())
    }

    // S41-A07 / S41-R07
    @Test
    fun robotoFlex_weightOutOfRange_isClampedTo100To1000() {
        val heavy = robotoFlex(weight = 1200)
        val light = robotoFlex(weight = 20)

        assertEquals(FontWeight(1000), heavy.weight)
        assertEquals(1000f, heavy.axes().getValue("wght"))
        assertEquals(FontWeight(100), light.weight)
        assertEquals(100f, light.axes().getValue("wght"))
    }

    // S41-A07 / S41-R07
    @Test
    fun robotoFlex_widthOutOfRange_isClampedTo25To151() {
        assertEquals(151f, robotoFlex(width = 400f).axes().getValue("wdth"))
        assertEquals(25f, robotoFlex(width = 0f).axes().getValue("wdth"))
        assertEquals(87.5f, robotoFlex(width = 87.5f).axes().getValue("wdth"))
    }

    // S41-A07 / S41-R07
    @Test
    fun robotoFlex_gradeOutOfRange_isClampedToMinus200To150() {
        assertEquals(150f, robotoFlex(grade = 500).axes().getValue("GRAD"))
        assertEquals(-200f, robotoFlex(grade = -900).axes().getValue("GRAD"))
    }

    // S41-A07 / S41-R07
    @Test
    fun robotoFlex_opticalSize_isOmittedByDefaultAndClampedWhenGiven() {
        assertFalse(robotoFlex().axes().containsKey("opsz"))
        assertEquals(144f, robotoFlex(opticalSize = 300f).axes().getValue("opsz"))
        assertEquals(8f, robotoFlex(opticalSize = 2f).axes().getValue("opsz"))
        assertEquals(36f, robotoFlex(opticalSize = 36f).axes().getValue("opsz"))
    }

    // S41-A07 / S41-R07
    @Test
    fun robotoFlexFamily_buildsOneFontPerWeightWithSharedWidthAndGrade() {
        val family = robotoFlexFamily(width = 140f, weights = listOf(300, 700), grade = 50) as FontListFontFamily

        assertEquals(listOf(FontWeight(300), FontWeight(700)), family.fonts.map { it.weight })
        family.fonts.forEach { font ->
            assertEquals(140f, font.axes().getValue("wdth"))
            assertEquals(50f, font.axes().getValue("GRAD"))
            assertEquals(font.weight.weight.toFloat(), font.axes().getValue("wght"))
        }
    }

    // S41-A07 / S41-R07: the default weights cover Material 3 (400, 500) plus the system bold text step (+300).
    @Test
    fun robotoFlexFamily_defaultWeights_includeMaterialWeightsAndTheirBoldTextVariants() {
        val weights = (robotoFlexFamily() as FontListFontFamily).fonts.map { it.weight.weight }

        listOf(400, 500, 700, 800).forEach { weight ->
            assertEquals("weight $weight in $weights", true, weight in weights)
        }
    }

    // S41-A07 / S41-R08
    @Test
    fun quantizeAxis_valueBetweenSteps_roundsToNearestStep() {
        assertEquals(136f, quantizeAxis(137.9f, 4f))
        assertEquals(140f, quantizeAxis(138.1f, 4f))
        assertEquals(850f, quantizeAxis(830f, 50f))
        assertEquals(-200f, quantizeAxis(-190f, 50f))
    }

    // S41-A07 / S41-R08
    @Test
    fun quantizeAxis_valueOnStep_isUnchanged() {
        assertEquals(124f, quantizeAxis(124f, 4f))
        assertEquals(900f, quantizeAxis(900f, 50f))
    }

    // S41-A07 / S41-R08
    @Test
    fun quantizeAxis_nonPositiveStep_throws() {
        assertThrows(IllegalArgumentException::class.java) { quantizeAxis(100f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { quantizeAxis(100f, -4f) }
    }
}
