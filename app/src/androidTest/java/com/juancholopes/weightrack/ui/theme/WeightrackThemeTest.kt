package com.juancholopes.weightrack.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class WeightrackThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ColorScheme.roles() = listOf(
        primary, onPrimary, primaryContainer, onPrimaryContainer,
        secondary, secondaryContainer, tertiary, tertiaryContainer,
        surface, onSurface, onSurfaceVariant, surfaceContainer, outline,
    )

    // S41-A04 / S41-R03: skipped below API 31, where dynamic color does not exist.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun dynamicColor_onApi31OrHigher_usesDynamicScheme() {
        lateinit var light: ColorScheme
        lateinit var dark: ColorScheme
        lateinit var expectedLight: ColorScheme
        lateinit var expectedDark: ColorScheme

        composeRule.setContent {
            val context = LocalContext.current
            expectedLight = dynamicLightColorScheme(context)
            expectedDark = dynamicDarkColorScheme(context)
            WeightrackTheme(darkTheme = false, dynamicColor = true) { light = MaterialTheme.colorScheme }
            WeightrackTheme(darkTheme = true, dynamicColor = true) { dark = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle {
            assertEquals(expectedLight.roles(), light.roles())
            assertEquals(expectedDark.roles(), dark.roles())
        }
    }

    // S41-R03
    @Test
    fun dynamicColorOff_usesFallbackSchemes() {
        lateinit var light: ColorScheme
        lateinit var dark: ColorScheme

        composeRule.setContent {
            WeightrackTheme(darkTheme = false, dynamicColor = false) { light = MaterialTheme.colorScheme }
            WeightrackTheme(darkTheme = true, dynamicColor = false) { dark = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle {
            assertSame(FallbackLightColors, light)
            assertSame(FallbackDarkColors, dark)
        }
    }

    // S41-R02, S41-R20
    @Test
    fun direction_providesItsTokensAndShapes() {
        val direction = VisualDirection.Scoreboard
        lateinit var provided: VisualDirection
        lateinit var spacing: Spacing
        lateinit var shapes: Shapes

        composeRule.setContent {
            WeightrackTheme(direction = direction) {
                provided = WeightrackTheme.direction
                spacing = WeightrackTheme.spacing
                shapes = MaterialTheme.shapes
            }
        }

        composeRule.runOnIdle {
            assertSame(direction, provided)
            assertEquals(direction.spacing, spacing)
            assertEquals(direction.shapes, shapes)
        }
    }
}
