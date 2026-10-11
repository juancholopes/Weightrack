package com.juancholopes.weightrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** The weight the system "bold text" setting adds (`Configuration.fontWeightAdjustment`). */
const val BoldTextWeightAdjustment = 300

/** Material 3 type scale with Roboto Flex at the given [width] (`wdth`). Read with `MaterialTheme.typography`. */
fun weightrackTypography(width: Float = 100f): Typography {
    val family = robotoFlexFamily(width = width)
    val material = Typography()
    return Typography(
        displayLarge = material.displayLarge.copy(fontFamily = family),
        displayMedium = material.displayMedium.copy(fontFamily = family),
        displaySmall = material.displaySmall.copy(fontFamily = family),
        headlineLarge = material.headlineLarge.copy(fontFamily = family),
        headlineMedium = material.headlineMedium.copy(fontFamily = family),
        headlineSmall = material.headlineSmall.copy(fontFamily = family),
        titleLarge = material.titleLarge.copy(fontFamily = family),
        titleMedium = material.titleMedium.copy(fontFamily = family),
        titleSmall = material.titleSmall.copy(fontFamily = family),
        bodyLarge = material.bodyLarge.copy(fontFamily = family),
        bodyMedium = material.bodyMedium.copy(fontFamily = family),
        bodySmall = material.bodySmall.copy(fontFamily = family),
        labelLarge = material.labelLarge.copy(fontFamily = family),
        labelMedium = material.labelMedium.copy(fontFamily = family),
        labelSmall = material.labelSmall.copy(fontFamily = family),
    )
}

/** Roboto Flex axes of one number style. Tunable per direction (S41-R19). */
@Immutable
data class NumberAxis(val size: TextUnit, val width: Float, val weight: Int, val grade: Int = 0)

/** Axes of the three number styles of a direction. */
@Immutable
data class NumberAxes(val hero: NumberAxis, val card: NumberAxis, val inline: NumberAxis)

/**
 * Styles for weights and reps, read with `WeightrackTheme.numbers`.
 *
 * - [hero]: the big number of the set logger.
 * - [card]: the weight on a card.
 * - [inline]: the sets in a list.
 *
 * Roboto Flex digits are tabular by default (the font has `pnum`, not `tnum`), so digits do not jump
 * when the value changes and no `fontFeatureSettings` is needed.
 */
@Immutable
data class NumberStyles(val hero: TextStyle, val card: TextStyle, val inline: TextStyle)

/**
 * One number style. Its family holds the exact weight plus the weight the "bold text" setting asks for,
 * so that setting still makes numbers heavier (S41-R15).
 */
fun numberStyle(axis: NumberAxis): TextStyle {
    val weight = axis.weight.coerceIn(RobotoFlexAxes.Weight)
    val boldTextWeight = (weight + BoldTextWeightAdjustment).coerceAtMost(RobotoFlexAxes.Weight.last)
    return TextStyle(
        fontFamily = robotoFlexFamily(
            width = axis.width,
            weights = listOf(weight, boldTextWeight).distinct(),
            grade = axis.grade,
        ),
        fontWeight = FontWeight(weight),
        fontSize = axis.size,
        lineHeight = axis.size * 1.15f,
        letterSpacing = 0.sp,
    )
}

fun numberStyles(axes: NumberAxes): NumberStyles = NumberStyles(
    hero = numberStyle(axes.hero),
    card = numberStyle(axes.card),
    inline = numberStyle(axes.inline),
)
