package com.juancholopes.weightrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.sp
import com.juancholopes.weightrack.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {

    private fun Typography.allStyles(): List<TextStyle> = listOf(
        displayLarge, displayMedium, displaySmall,
        headlineLarge, headlineMedium, headlineSmall,
        titleLarge, titleMedium, titleSmall,
        bodyLarge, bodyMedium, bodySmall,
        labelLarge, labelMedium, labelSmall,
    )

    private fun TextStyle.fonts(): List<ResourceFont> =
        (fontFamily as FontListFontFamily).fonts.map { it as ResourceFont }

    private fun ResourceFont.axis(name: String): Float =
        variationSettings.settings.single { it.axisName == name }.toVariationValue(null)

    // S41-R07
    @Test
    fun weightrackTypography_everyStyle_usesRobotoFlexWithTheGivenWidth() {
        weightrackTypography(width = 120f).allStyles().forEach { style ->
            style.fonts().forEach { font ->
                assertEquals(R.font.roboto_flex, font.resId)
                assertEquals(120f, font.axis("wdth"))
            }
        }
    }

    // S41-R07: the text keeps the Material 3 type scale; only the family changes.
    @Test
    fun weightrackTypography_everyStyle_keepsTheMaterialScale() {
        val material = Typography().allStyles()

        weightrackTypography().allStyles().zip(material).forEach { (style, expected) ->
            assertEquals(expected.fontSize, style.fontSize)
            assertEquals(expected.lineHeight, style.lineHeight)
            assertEquals(expected.fontWeight, style.fontWeight)
            assertEquals(expected.letterSpacing, style.letterSpacing)
        }
    }

    // S41-R07
    @Test
    fun numberStyle_usesTheSizeWidthWeightAndGradeOfItsAxis() {
        val style = numberStyle(NumberAxis(size = 64.sp, width = 151f, weight = 800, grade = 20))

        assertEquals(64.sp, style.fontSize)
        assertEquals(FontWeight(800), style.fontWeight)
        style.fonts().forEach { font ->
            assertEquals(151f, font.axis("wdth"))
            assertEquals(20f, font.axis("GRAD"))
        }
    }

    // S41-R15: the system "bold text" setting adds 300 to the weight, so the family carries that variant.
    @Test
    fun numberStyle_family_hasTheExactWeightAndItsBoldTextVariant() {
        assertEquals(listOf(300, 600), numberStyle(NumberAxis(16.sp, 100f, 300)).fonts().map { it.weight.weight })
        assertEquals(listOf(800, 1000), numberStyle(NumberAxis(16.sp, 100f, 800)).fonts().map { it.weight.weight })
        assertEquals(listOf(1000), numberStyle(NumberAxis(16.sp, 100f, 1000)).fonts().map { it.weight.weight })
    }

    // S41-R07
    @Test
    fun numberStyles_buildsHeroCardAndInlineFromTheirAxes() {
        val axes = NumberAxes(
            hero = NumberAxis(64.sp, 151f, 800),
            card = NumberAxis(32.sp, 140f, 700),
            inline = NumberAxis(16.sp, 100f, 600),
        )

        val styles = numberStyles(axes)

        assertEquals(numberStyle(axes.hero), styles.hero)
        assertEquals(numberStyle(axes.card), styles.card)
        assertEquals(numberStyle(axes.inline), styles.inline)
    }
}
