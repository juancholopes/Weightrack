package com.juancholopes.weightrack.ui.theme.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import kotlin.math.roundToInt

/**
 * A weight or repetitions number drawn with `NumberStyles` (Spec 41 "Componentes compartidos"). Stateless
 * and domain-free: the caller formats the text with `Formats.kt` and passes the unit apart when it wants it
 * smaller than the number.
 *
 * When the text changes, each digit rolls up if it grew and down if it shrank (S42-R27). With reduced
 * motion the number changes at once (S41-R10). The number never wraps: if it is wider than the space it is
 * given, for example at a very large font, it shrinks to fit that width instead of being cut.
 *
 * It is one accessibility node that reads [number] and [unit], or [spokenText] when given.
 *
 * @param number the digits as shown: "62,5", "10" or "—".
 * @param unit shown next to the number in a smaller size, such as "kg"; `null` for none.
 * @param style the `NumberStyles` entry: `hero` in the logger, `card` on a card, `inline` in a list.
 * @param spokenText what TalkBack reads, such as "62,5 kilogramos"; the visible text by default.
 */
@Composable
fun WeightNumber(
    number: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = WeightrackTheme.numbers.hero,
    color: Color = LocalContentColor.current,
    spokenText: String? = null,
) {
    val visibleText = if (unit == null) number else "$number $unit"
    val reducedMotion = WeightrackTheme.motion.reducedMotion
    Row(
        modifier = modifier
            .fitWidth()
            .clearAndSetSemantics {
                this.text = AnnotatedString(visibleText)
                spokenText?.let { contentDescription = it }
            },
        verticalAlignment = Alignment.Bottom,
    ) {
        if (reducedMotion) {
            Text(text = number, style = style, color = color, maxLines = 1, softWrap = false)
        } else {
            // Each digit is keyed by its position from the end, so the units digit stays the same slot when the
            // number gains or loses a digit.
            number.forEachIndexed { index, char ->
                key(number.length - index) { RollingChar(char = char, style = style, color = color) }
            }
        }
        unit?.let {
            val descent = with(LocalDensity.current) { (style.fontSize * UnitDescentFraction).toDp() }
            Text(
                text = it,
                modifier = Modifier.padding(start = style.fontSizeInDp() / UnitGapDivisor, bottom = descent),
                style = MaterialTheme.typography.titleMedium,
                color = color,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** One character that rolls to the next: up when it is greater than the previous one, down otherwise (S42-R27). */
@Composable
private fun RollingChar(char: Char, style: TextStyle, color: Color) {
    val motion = WeightrackTheme.motion
    AnimatedContent(
        targetState = char,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            val enter = slideInVertically(motion.defaultSpatialSpec()) { height -> height * direction } +
                fadeIn(motion.fastEffectsSpec())
            val exit = slideOutVertically(motion.defaultSpatialSpec()) { height -> -height * direction } +
                fadeOut(motion.fastEffectsSpec())
            (enter togetherWith exit).using(SizeTransform(clip = true) { _, _ -> snap() })
        },
        label = "digit",
    ) { digit ->
        Text(text = digit.toString(), style = style, color = color, maxLines = 1, softWrap = false)
    }
}

/** The gap between the number and its unit, as a fraction of the font size: [UnitGapDivisor] is 1 / that. */
private const val UnitGapDivisor = 8

/** How far the unit rises above the bottom of the line so it sits on the baseline of the digits. */
private const val UnitDescentFraction = 0.12f

@Composable
private fun TextStyle.fontSizeInDp() = with(LocalDensity.current) { fontSize.toDp() }

/**
 * Measures its content as wide as it wants and, when that is wider than the space it has, scales it down to
 * fit. It never grows it. Used so a number at a large font scale shrinks instead of overflowing.
 */
private fun Modifier.fitWidth(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val scale = if (constraints.hasBoundedWidth && placeable.width > constraints.maxWidth) {
        constraints.maxWidth.toFloat() / placeable.width
    } else {
        1f
    }
    val width = (placeable.width * scale).roundToInt()
    val height = (placeable.height * scale).roundToInt()
    layout(width, height) {
        placeable.placeWithLayer(IntOffset.Zero) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

// --- Previews ---

@ThemePreviews
@Composable
private fun WeightNumberHeroPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(WeightrackTheme.spacing.l), verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.l)) {
                WeightNumber(number = "62,5", unit = "kg")
                WeightNumber(number = "10")
                WeightNumber(number = "—")
                WeightNumber(number = "60", unit = "kg", style = WeightrackTheme.numbers.card)
            }
        }
    }
}

/** Tap the number to see the digits roll. */
@ThemePreviews
@Composable
private fun WeightNumberRollingPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            var tapped by remember { mutableIntStateOf(0) }
            val grams = 60_000 + tapped * 1_250
            WeightNumber(
                number = (grams / 1000f).toString().trimEnd('0').trimEnd('.').replace('.', ','),
                unit = "kg",
                modifier = Modifier
                    .padding(WeightrackTheme.spacing.l)
                    .clickable { tapped += 1 },
            )
        }
    }
}
