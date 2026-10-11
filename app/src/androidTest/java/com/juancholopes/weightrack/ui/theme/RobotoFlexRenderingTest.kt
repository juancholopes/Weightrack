package com.juancholopes.weightrack.ui.theme

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bundled Roboto Flex loads on the device and its axes change the rendered text (S41-A08). */
class RobotoFlexRenderingTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(context),
        defaultDensity = Density(context),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )

    private fun measuredWidth(family: FontFamily, weight: FontWeight = FontWeight.Normal): Int =
        measurer.measure(SAMPLE, TextStyle(fontFamily = family, fontWeight = weight, fontSize = 32.sp)).size.width

    // S41-A08 / S41-R05, S41-R07
    @Test
    fun width151_isWiderThanWidth25() {
        val wide = measuredWidth(robotoFlexFamily(width = 151f))
        val condensed = measuredWidth(robotoFlexFamily(width = 25f))

        assertTrue("wdth 151 = $wide px, wdth 25 = $condensed px", wide > condensed)
    }

    // S41-A08 / S41-R05, S41-R07
    @Test
    fun weight900_isWiderThanWeight100() {
        val heavy = measuredWidth(robotoFlexFamily(weights = listOf(900)), FontWeight(900))
        val light = measuredWidth(robotoFlexFamily(weights = listOf(100)), FontWeight(100))

        assertTrue("wght 900 = $heavy px, wght 100 = $light px", heavy > light)
    }

    // S41-R07: Roboto Flex has no `tnum`; its default digits must already be tabular at the hero axes.
    @Test
    fun digits_atHeroAxes_haveEqualWidths() {
        val style = numberStyle(VisualDirection.Expressive.numbers.hero)

        val widths = (0..9).map { digit -> measurer.measure("$digit".repeat(4), style).size.width }

        assertEquals("digit widths $widths", 1, widths.distinct().size)
    }

    private companion object {
        const val SAMPLE = "Press de banca 62,5"
    }
}
