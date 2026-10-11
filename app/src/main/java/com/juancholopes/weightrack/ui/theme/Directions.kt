package com.juancholopes.weightrack.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.sp

/** How cards separate from the background in a direction. Components read it from the direction. */
enum class CardStyle {
    /** Flat card on a tonal container (`primaryContainer`, `secondaryContainer`, `tertiaryContainer`). */
    Tonal,

    /** `surfaceContainerHighest` with a `primary` edge on the start side. */
    AccentEdge,

    /** `surface` with a thin `outlineVariant` border. */
    Outlined,
}

/**
 * A visual direction: a preset of tunable values (S41-R20). Colors always come from the color scheme;
 * a direction only chooses shapes, spacing, text and number axes, how cards are colored and motion.
 *
 * Compare the presets in `DesignLab`. The app uses [DefaultDirection].
 */
@Immutable
data class VisualDirection(
    val name: String,
    val shapes: Shapes,
    val spacing: Spacing,
    /** `wdth` of the text type scale. */
    val textWidth: Float,
    val numbers: NumberAxes,
    val cardStyle: CardStyle,
    val motion: MotionTokens,
) {
    companion object {
        /** A. Expressive: tonal cards, very rounded corners, wide and heavy numbers, springs. */
        val Expressive = VisualDirection(
            name = "Expressive",
            shapes = WeightrackShapes.Expressive,
            spacing = Spacing.Expressive,
            textWidth = 100f,
            numbers = NumberAxes(
                hero = NumberAxis(size = 64.sp, width = 151f, weight = 800),
                card = NumberAxis(size = 32.sp, width = 140f, weight = 700),
                inline = NumberAxis(size = 16.sp, width = 100f, weight = 600),
            ),
            cardStyle = CardStyle.Tonal,
            motion = MotionTokens.Expressive,
        )

        /** B. Scoreboard: high-contrast surfaces with a colored edge, condensed and very heavy numbers. */
        val Scoreboard = VisualDirection(
            name = "Scoreboard",
            shapes = WeightrackShapes.Scoreboard,
            spacing = Spacing.Scoreboard,
            textWidth = 85f,
            numbers = NumberAxes(
                hero = NumberAxis(size = 72.sp, width = 30f, weight = 900),
                card = NumberAxis(size = 36.sp, width = 40f, weight = 900),
                inline = NumberAxis(size = 16.sp, width = 50f, weight = 800),
            ),
            cardStyle = CardStyle.AccentEdge,
            motion = MotionTokens.Standard.withoutBounce().copy(
                shortDurationMillis = 120,
                mediumDurationMillis = 200,
                easing = MotionTokens.StandardDecelerateEasing,
            ),
        )

        /** C. Notebook: calm surfaces with thin borders, large and light numbers that gain emphasis with grade. */
        val Notebook = VisualDirection(
            name = "Notebook",
            shapes = WeightrackShapes.Notebook,
            spacing = Spacing.Notebook,
            textWidth = 100f,
            numbers = NumberAxes(
                hero = NumberAxis(size = 64.sp, width = 110f, weight = 300, grade = 100),
                card = NumberAxis(size = 32.sp, width = 110f, weight = 300, grade = 50),
                inline = NumberAxis(size = 16.sp, width = 110f, weight = 400),
            ),
            cardStyle = CardStyle.Outlined,
            motion = MotionTokens.Standard.withoutBounce().copy(
                shortDurationMillis = 250,
                mediumDurationMillis = 350,
                easing = MotionTokens.StandardEasing,
            ),
        )

        val presets: List<VisualDirection> = listOf(Expressive, Scoreboard, Notebook)
    }
}

/** The direction of the app. Changing direction is changing this line (S41-R20). */
val DefaultDirection: VisualDirection = VisualDirection.Expressive
