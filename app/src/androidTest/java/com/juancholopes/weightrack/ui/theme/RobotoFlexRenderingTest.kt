package com.juancholopes.weightrack.ui.theme

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bundled Roboto Flex loads on the device and renders (S41-A08).
 *
 * The tests that its axes change the rendered text (`wdth` 151 wider than 25, `wght` 900 wider than 100) are removed
 * on purpose. Owner decision, 2026-10-14: the app only needs Roboto Flex, not runtime axis changes, and HyperOS ignores
 * axes set on `res/font` fonts. They cannot stay as `@Ignore`: the AGP 9.4.1 test report writes any ignored test as an
 * empty `<failure>` in its XML. If axes are needed, move the font to `assets/` and load it with
 * `Font(path, assetManager, ...)`, then bring the two tests back from commit a81efbd.
 */
class RobotoFlexRenderingTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(context),
        defaultDensity = Density(context),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )

    // S41-R07: Roboto Flex has no `tnum`; its default digits must already be tabular at the hero axes.
    @Test
    fun digits_atHeroAxes_haveEqualWidths() {
        val style = numberStyle(VisualDirection.Expressive.numbers.hero)

        val widths = (0..9).map { digit -> measurer.measure("$digit".repeat(4), style).size.width }

        assertEquals("digit widths $widths", 1, widths.distinct().size)
    }
}
