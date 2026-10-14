package com.juancholopes.weightrack.feature.session.ui.logger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.WeightNumber
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * What [ValueAdjuster] draws and reads for the current value: the digits, the unit drawn next to them and the
 * reading for TalkBack with its unit, such as "62,5 kilogramos". A value that is not fixed has `"—"`, no unit
 * and the reading "Sin fijar". The caller builds it with `Formats.kt`, so the control never writes a format.
 */
data class ValueDisplay(val number: String, val unit: String?, val spoken: String)

/** The events of a [ValueAdjuster]; each one is a call of the matching `SetLoggerViewModel` function. */
class AdjusterActions(
    val onDecrease: () -> Unit,
    val onIncrease: () -> Unit,
    val onSlide: (Int) -> Unit,
    val onSlideFinished: () -> Unit,
    val onNumberClick: () -> Unit,
)

/**
 * The control of a weight or of the repetitions (Spec 42, "Control del peso y las repeticiones"), used twice by
 * the set logger. Stateless: [input] says the value, the window of the slider and which buttons are disabled
 * (S42-R19), and every change goes out through [actions].
 *
 * It has the big number, which is a button that opens the typing dialog, the buttons − and +, which repeat while
 * they are held, and a `Slider` over [ValueWindow]: the gross adjustment, which only produces stops of the
 * window. The slider draws the value at its exact place, even between two stops, and while the value is not
 * fixed its handle rests at the center of the window.
 *
 * TalkBack gets the label and the value with its unit on the number, a label on each button and the value as the
 * state of the slider; the number says how to type the value (S42-R20).
 *
 * @param label "Peso", "Peso adicional" or "Repeticiones"; also the heading that lets TalkBack jump to the control.
 * @param display the number and its readings.
 * @param unsetHint what is written under the "—" while the value is not fixed, such as "Fija el peso".
 */
@Composable
fun ValueAdjuster(
    label: String,
    input: ValueInput,
    display: ValueDisplay,
    unsetHint: String,
    decreaseDescription: String,
    increaseDescription: String,
    actions: AdjusterActions,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val window = input.window
    val numberDescription = stringResource(R.string.logger_value_description, label, display.spoken)
    val typeLabel = stringResource(R.string.logger_type_click_label)
    // The window can start where the value is not (a value between stops); the handle never leaves the track.
    val sliderValue = (input.value ?: ((window.start + window.end) / 2)).coerceIn(window.start, window.end).toFloat()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = label,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            StepButton(
                iconRes = R.drawable.ic_remove,
                description = decreaseDescription,
                enabled = input.canDecrease,
                onStep = actions.onDecrease,
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clickable(onClickLabel = typeLabel, role = Role.Button, onClick = actions.onNumberClick)
                        .semantics(mergeDescendants = true) { contentDescription = numberDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    WeightNumber(number = display.number, unit = display.unit)
                }
                if (!input.isFixed) {
                    Text(
                        text = unsetHint,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StepButton(
                iconRes = R.drawable.ic_add,
                description = increaseDescription,
                enabled = input.canIncrease,
                onStep = actions.onIncrease,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { actions.onSlide(it.roundToInt()) },
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = display.spoken
            },
            valueRange = window.start.toFloat()..window.end.toFloat(),
            steps = window.marks,
            onValueChangeFinished = actions.onSlideFinished,
        )
    }
}

/**
 * A "−" or "+" button: one tap steps once, and holding it repeats after a short pause as a shortcut (Spec 42).
 * When the hold repeated, releasing does not step once more. It is at least 48 dp (S41-R12) and its description
 * says what it does and by how much.
 */
@Composable
private fun StepButton(iconRes: Int, description: String, enabled: Boolean, onStep: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)
    var heldRepeatedly by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            heldRepeatedly = false
            delay(HoldDelayMillis)
            while (true) {
                heldRepeatedly = true
                currentOnStep()
                delay(RepeatIntervalMillis)
            }
        }
    }

    FilledTonalIconButton(
        onClick = {
            if (!heldRepeatedly) onStep()
            heldRepeatedly = false
        },
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = description)
    }
}

private val MinTouchTarget = 48.dp

/** Tunable: how long a button is held before it starts to repeat, and the time between two repeats. */
private const val HoldDelayMillis = 450L
private const val RepeatIntervalMillis = 90L

@ThemePreviews
@Composable
private fun ValueAdjusterPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(WeightrackTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.l),
            ) {
                val none = AdjusterActions({}, {}, {}, {}, {})
                ValueAdjuster(
                    label = "Peso",
                    input = ValueInput(62_500, weightWindow(62_500), canDecrease = true, canIncrease = true),
                    display = ValueDisplay("62,5", "kg", "62,5 kilogramos"),
                    unsetHint = "Fija el peso",
                    decreaseDescription = "Bajar 0,25 kilogramos",
                    increaseDescription = "Subir 0,25 kilogramos",
                    actions = none,
                )
                ValueAdjuster(
                    label = "Repeticiones",
                    input = ValueInput(null, repsWindow(REPS_DEFAULT_REFERENCE), canDecrease = true, canIncrease = true),
                    display = ValueDisplay("—", null, "Sin fijar"),
                    unsetHint = "Fija las repeticiones",
                    decreaseDescription = "Bajar una repetición",
                    increaseDescription = "Subir una repetición",
                    actions = none,
                )
            }
        }
    }
}
