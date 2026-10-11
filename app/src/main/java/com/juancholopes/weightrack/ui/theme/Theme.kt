package com.juancholopes.weightrack.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Tokens Material 3 does not have (D1): they travel together through [LocalWeightrackTokens].
 * Colors, text typography and shapes live in `MaterialTheme`.
 */
@Immutable
data class WeightrackTokens(
    val direction: VisualDirection,
    val spacing: Spacing,
    val numbers: NumberStyles,
    val motion: MotionTokens,
)

/** Builds the tokens of a [direction]; with [reducedMotion], its motion for reduced motion (S41-R10). */
fun weightrackTokens(direction: VisualDirection, reducedMotion: Boolean = false): WeightrackTokens = WeightrackTokens(
    direction = direction,
    spacing = direction.spacing,
    numbers = numberStyles(direction.numbers),
    motion = if (reducedMotion) direction.motion.forReducedMotion() else direction.motion,
)

val LocalWeightrackTokens = staticCompositionLocalOf { weightrackTokens(DefaultDirection) }

/**
 * The only entry point of the theme (S41-R02). `MainActivity` wraps the shell with it and every preview
 * does too.
 *
 * @param darkTheme follows the system; there is no in-app selector (S41-R04).
 * @param dynamicColor Material You dynamic color on API 31+; the fallback scheme otherwise (S41-R03).
 * @param direction the visual preset; the app uses [DefaultDirection] (S41-R20).
 */
@Composable
fun WeightrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    direction: VisualDirection = DefaultDirection,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // selectColorScheme calls these providers only on API 31+ (ColorSchemesTest).
    @SuppressLint("NewApi")
    val dynamicLight = { dynamicLightColorScheme(context) }

    @SuppressLint("NewApi")
    val dynamicDark = { dynamicDarkColorScheme(context) }
    val colorScheme = selectColorScheme(
        sdkInt = Build.VERSION.SDK_INT,
        dynamicColor = dynamicColor,
        darkTheme = darkTheme,
        dynamicLight = dynamicLight,
        dynamicDark = dynamicDark,
    )
    val reducedMotion = rememberReducedMotion()
    val tokens = remember(direction, reducedMotion) { weightrackTokens(direction, reducedMotion) }
    val typography = remember(direction.textWidth) { weightrackTypography(direction.textWidth) }

    CompositionLocalProvider(LocalWeightrackTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = direction.shapes,
            typography = typography,
            content = content,
        )
    }
}

/** Readers of the Weightrack tokens, like `MaterialTheme.colorScheme`: `WeightrackTheme.spacing.l`. */
object WeightrackTheme {
    val direction: VisualDirection
        @Composable @ReadOnlyComposable
        get() = LocalWeightrackTokens.current.direction

    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalWeightrackTokens.current.spacing

    val numbers: NumberStyles
        @Composable @ReadOnlyComposable
        get() = LocalWeightrackTokens.current.numbers

    val motion: MotionTokens
        @Composable @ReadOnlyComposable
        get() = LocalWeightrackTokens.current.motion
}
