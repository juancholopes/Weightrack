package com.juancholopes.weightrack.ui.theme.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import kotlin.math.roundToInt

/**
 * A row of tabs in pills (Spec 41 "Componentes compartidos"): the selected one is filled and the others are
 * text only. Stateless: [selectedIndex] and [onSelect] belong to the caller's `UiState`. Each tab has the
 * `Tab` role and the row is a selectable group, and each measures at least 48 dp tall (S41-R12).
 *
 * The fill moves to the new tab with a fast spatial spring and the label colors cross-fade; with reduced
 * motion both change at once (Spec 42, "Transiciones").
 *
 * @param labels the text of each tab.
 */
@Composable
fun PillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val motion = WeightrackTheme.motion
    val density = LocalDensity.current
    val tabBounds = remember { mutableStateMapOf<Int, Rect>() }
    val fill = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val selectedBounds = tabBounds[selectedIndex]

    LaunchedEffect(selectedBounds, motion.reducedMotion) {
        if (selectedBounds == null) return@LaunchedEffect
        if (motion.reducedMotion || fill.value == Rect.Zero) {
            fill.snapTo(selectedBounds)
        } else {
            fill.animateTo(selectedBounds, motion.fastSpatialSpec())
        }
    }

    Box(modifier = modifier) {
        val rect = fill.value
        if (rect != Rect.Zero) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                    .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            )
        }
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = motion.fastEffectsSpec(),
                    label = "tab label",
                )
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates -> tabBounds[index] = coordinates.boundsInParent() }
                        .heightIn(min = MinTabHeight)
                        .clip(CircleShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = spacing.xl, vertical = spacing.s),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = MaterialTheme.typography.titleSmall, color = labelColor, maxLines = 1)
                }
            }
        }
    }
}

private val MinTabHeight = 48.dp

@ThemePreviews
@Composable
private fun PillTabsPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            var selected by remember { mutableIntStateOf(0) }
            PillTabs(
                labels = listOf("Hoy", "Ejercicios"),
                selectedIndex = selected,
                onSelect = { selected = it },
                modifier = Modifier.padding(WeightrackTheme.spacing.l),
            )
        }
    }
}
