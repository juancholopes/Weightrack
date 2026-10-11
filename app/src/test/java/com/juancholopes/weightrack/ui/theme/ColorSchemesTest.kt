package com.juancholopes.weightrack.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemesTest {

    private val dynamicLight = lightColorScheme(primary = Color.Red)
    private val dynamicDark = darkColorScheme(primary = Color.Blue)

    private fun select(sdkInt: Int, dynamicColor: Boolean, darkTheme: Boolean): ColorScheme =
        selectColorScheme(
            sdkInt = sdkInt,
            dynamicColor = dynamicColor,
            darkTheme = darkTheme,
            dynamicLight = { dynamicLight },
            dynamicDark = { dynamicDark },
        )

    // S41-A02 / S41-R03
    @Test
    fun selectColorScheme_api31WithDynamicColorLight_returnsDynamicLight() {
        assertSame(dynamicLight, select(sdkInt = 31, dynamicColor = true, darkTheme = false))
    }

    // S41-A02 / S41-R03
    @Test
    fun selectColorScheme_api31WithDynamicColorDark_returnsDynamicDark() {
        assertSame(dynamicDark, select(sdkInt = 31, dynamicColor = true, darkTheme = true))
    }

    // S41-A02 / S41-R03
    @Test
    fun selectColorScheme_api30WithDynamicColor_returnsFallback() {
        assertSame(FallbackLightColors, select(sdkInt = 30, dynamicColor = true, darkTheme = false))
        assertSame(FallbackDarkColors, select(sdkInt = 30, dynamicColor = true, darkTheme = true))
    }

    // S41-A02 / S41-R03
    @Test
    fun selectColorScheme_dynamicColorDisabled_returnsFallbackLight() {
        assertSame(FallbackLightColors, select(sdkInt = 36, dynamicColor = false, darkTheme = false))
    }

    // S41-A02 / S41-R03
    @Test
    fun selectColorScheme_darkWithoutDynamicColor_returnsFallbackDark() {
        assertSame(FallbackDarkColors, select(sdkInt = 36, dynamicColor = false, darkTheme = true))
    }

    // S41-A02 / S41-R03: dynamicLightColorScheme crashes below API 31, so it must not be called there.
    @Test
    fun selectColorScheme_belowApi31_neverCallsDynamicProviders() {
        val scheme = selectColorScheme(
            sdkInt = 26,
            dynamicColor = true,
            darkTheme = false,
            dynamicLight = { error("dynamic light called below API 31") },
            dynamicDark = { error("dynamic dark called below API 31") },
        )

        assertSame(FallbackLightColors, scheme)
    }

    // S41-A03 / S41-R03, S41-R15
    @Test
    fun fallbackSchemes_textPairs_meetContrast() {
        listOf("light" to FallbackLightColors, "dark" to FallbackDarkColors).forEach { (name, scheme) ->
            val pairs = listOf(
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onSurface/surface" to (scheme.onSurface to scheme.surface),
                "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
                "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondaryContainer/secondaryContainer" to
                    (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            )
            pairs.forEach { (pair, colors) ->
                val ratio = contrastRatio(colors.first, colors.second)
                assertTrue("$name $pair contrast is $ratio, expected >= 4.5", ratio >= 4.5f)
            }
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
