package com.juancholopes.weightrack.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.juancholopes.weightrack.R
import kotlin.math.roundToInt

/*
 * Roboto Flex, the only font of the app (S41-R05, S41-R07).
 *
 * res/font/roboto_flex.ttf is the full variable font from google/fonts (ofl/robotoflex, version 3.200),
 * licensed under the SIL OFL 1.1 (assets/licenses/roboto_flex_ofl.txt). Every font of the app is built
 * here, so every axis value is clamped to the range the font supports.
 */

/** Ranges of the exposed axes, from the METADATA.pb of google/fonts (D5). */
object RobotoFlexAxes {
    /** `wght`. */
    val Weight = 100..1000

    /** `wdth`: 25 is the most condensed, 151 the widest. */
    val Width = 25f..151f

    /** `GRAD`: weight without changing the width, so the text does not move. */
    val Grade = -200..150

    /** `opsz`: optional. When it is not set, the font uses its default (14). */
    val OpticalSize = 8f..144f
}

/**
 * Weights of a text family: the Material 3 weights (400 and 500), a lighter one, and the variants that
 * the system "bold text" setting asks for (+300), so it keeps working with a variable font.
 */
val DefaultRobotoFlexWeights: List<Int> = listOf(300, 400, 500, 700, 800, 900)

/** One Roboto Flex instance. Out-of-range values are clamped to [RobotoFlexAxes]. */
fun robotoFlex(weight: Int = 400, width: Float = 100f, grade: Int = 0, opticalSize: Float? = null): Font {
    val clampedWeight = weight.coerceIn(RobotoFlexAxes.Weight)
    val settings = buildList {
        add(FontVariation.weight(clampedWeight))
        add(FontVariation.width(width.coerceIn(RobotoFlexAxes.Width)))
        add(FontVariation.grade(grade.coerceIn(RobotoFlexAxes.Grade)))
        if (opticalSize != null) add(FontVariation.Setting("opsz", opticalSize.coerceIn(RobotoFlexAxes.OpticalSize)))
    }
    return Font(
        resId = R.font.roboto_flex,
        weight = FontWeight(clampedWeight),
        variationSettings = FontVariation.Settings(*settings.toTypedArray()),
    )
}

/** A family with one instance per weight, all with the same width, grade and optical size. */
fun robotoFlexFamily(
    width: Float = 100f,
    weights: List<Int> = DefaultRobotoFlexWeights,
    grade: Int = 0,
    opticalSize: Float? = null,
): FontFamily = FontFamily(weights.map { robotoFlex(it, width, grade, opticalSize) })

/**
 * Rounds an animated axis value to steps (S41-R08), for example 4 for `wdth` and 50 for `wght`, so an
 * animation creates few typefaces and fits in the 16-entry typeface cache of Compose.
 */
fun quantizeAxis(value: Float, step: Float): Float {
    require(step > 0f) { "step must be > 0. Actual: $step" }
    return (value / step).roundToInt() * step
}
