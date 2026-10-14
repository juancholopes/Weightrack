package com.juancholopes.weightrack.ui.theme.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.ui.theme.CardStyle
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme

/**
 * The color family of a card. A card knows no domain, so the screen chooses the tone (for example by equipment,
 * in direction A) and the card paints it with the roles of the color scheme and the style of the direction.
 */
enum class CardTone { Primary, Secondary, Tertiary, Neutral }

/** One entry of the menu of a card. It is also an accessibility action of the card (Spec 42, "Tarjeta"). */
data class ExerciseCardAction(val label: String, val onClick: () -> Unit)

/**
 * Big rounded card of an exercise (Spec 41 "Componentes compartidos"). Stateless and domain-free: it shows a
 * [title], a [subtitle], a slot for the weight and a [footer], and reports the tap and the menu actions.
 *
 * It is a single node for TalkBack: it reads [spokenDescription], offers [tapLabel] as its click action and
 * every entry of [menuActions] as a custom action, so the menu button needs no node of its own. Pressing it
 * scales it to 0,96 and changes its tone; with reduced motion only the tone changes (S42-R27, S41-R10).
 *
 * @param tone the color family; the direction decides if it fills the card, marks its edge or is only a border.
 * @param weight the number of the card, usually a [WeightNumber]; `null` for a card without one.
 * @param badge a short mark next to the footer, such as "Archivado"; it is text, so it never depends on color.
 * @param menuActions the entries of the menu; empty hides the menu button.
 * @param menuDescription what TalkBack calls the menu button.
 */
@Composable
fun ExerciseCard(
    title: String,
    subtitle: String,
    tone: CardTone,
    onClick: () -> Unit,
    spokenDescription: String,
    tapLabel: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    badge: String? = null,
    weight: (@Composable () -> Unit)? = null,
    menuActions: List<ExerciseCardAction> = emptyList(),
    menuDescription: String = "",
) {
    val spacing = WeightrackTheme.spacing
    val motion = WeightrackTheme.motion
    val style = WeightrackTheme.direction.cardStyle
    val colors = toneColors(tone, style)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val handleClick = onClick

    val scale by animateFloatAsState(
        targetValue = if (pressed && !motion.reducedMotion) PressedScale else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "card scale",
    )
    val container by animateColorAsState(
        targetValue = if (pressed) lerp(colors.container, colors.content, PressedToneMix) else colors.container,
        animationSpec = motion.fastEffectsSpec(),
        label = "card tone",
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = spokenDescription
                // The visible texts stay findable by what they say; TalkBack reads the description above.
                this[SemanticsProperties.Text] = listOfNotNull(title, subtitle, footer, badge).map { AnnotatedString(it) }
                onClick(label = tapLabel) {
                    handleClick()
                    true
                }
                customActions = menuActions.map { action ->
                    CustomAccessibilityAction(action.label) {
                        action.onClick()
                        true
                    }
                }
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = colors.content),
        border = if (style == CardStyle.Outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = MinCardHeight)
                .then(if (style == CardStyle.AccentEdge) Modifier.accentEdge(colors.accent) else Modifier)
                .padding(start = spacing.l, top = spacing.l, end = spacing.s, bottom = spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = spacing.s, end = spacing.xs),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (menuActions.isNotEmpty()) {
                    CardMenu(actions = menuActions, description = menuDescription)
                } else {
                    Spacer(modifier = Modifier.width(spacing.m))
                }
            }
            Text(
                text = subtitle,
                modifier = Modifier.padding(end = spacing.m),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            weight?.let {
                Box(modifier = Modifier.padding(top = spacing.s, end = spacing.m)) { it() }
            }
            if (footer != null || badge != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    footer?.let {
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    badge?.let { Badge(it) }
                }
            }
        }
    }
}

/** The "more options" button of a card and its menu. The menu state is local: whether it is open is no one else's. */
@Composable
private fun CardMenu(actions: List<ExerciseCardAction>, description: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        // IconButton measures 48 dp (S41-R12).
        IconButton(onClick = { expanded = true }) {
            Icon(painter = painterResource(R.drawable.ic_more_vert), contentDescription = description)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/** A small written mark, such as "Archivado": text on a tonal pill, never color alone (S40-R49). */
@Composable
private fun Badge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = BadgeSurfaceAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = WeightrackTheme.spacing.s, vertical = WeightrackTheme.spacing.xxs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private class ToneColors(val container: Color, val content: Color, val accent: Color)

/** The colors of [tone] under the card style of the direction; every pair comes from the color scheme (S41-R15). */
@Composable
private fun toneColors(tone: CardTone, style: CardStyle): ToneColors {
    val scheme = MaterialTheme.colorScheme
    val tonal = when (tone) {
        CardTone.Primary -> ToneColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
        CardTone.Secondary -> ToneColors(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary)
        CardTone.Tertiary -> ToneColors(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary)
        CardTone.Neutral -> ToneColors(scheme.surfaceContainerHighest, scheme.onSurface, scheme.outline)
    }
    return when (style) {
        CardStyle.Tonal -> tonal
        CardStyle.AccentEdge -> ToneColors(scheme.surfaceContainerHighest, scheme.onSurface, tonal.accent)
        CardStyle.Outlined -> ToneColors(scheme.surface, scheme.onSurface, tonal.accent)
    }
}

/** A thin bar of [color] on the start edge of the card. */
private fun Modifier.accentEdge(color: Color): Modifier = drawBehind {
    drawRect(color = color, size = Size(width = AccentEdgeWidth.toPx(), height = size.height))
}

private val MinCardHeight = 156.dp
private val AccentEdgeWidth = 4.dp

/** Tunable: how small a pressed card gets (Spec 42, "Transiciones") and how far its tone moves toward its content color. */
private const val PressedScale = 0.96f
private const val PressedToneMix = 0.12f
private const val BadgeSurfaceAlpha = 0.6f

// --- Previews ---

@Composable
private fun PreviewCard(tone: CardTone, title: String, subtitle: String, number: String?, footer: String, badge: String? = null) {
    ExerciseCard(
        title = title,
        subtitle = subtitle,
        tone = tone,
        onClick = {},
        spokenDescription = "$title. $subtitle.",
        tapLabel = "Registrar una serie",
        footer = footer,
        badge = badge,
        weight = number?.let { { WeightNumber(number = it, unit = "kg", style = WeightrackTheme.numbers.card) } },
        menuActions = listOf(ExerciseCardAction("Editar ejercicio") {}),
        menuDescription = "Más opciones",
        modifier = Modifier.fillMaxWidth(),
    )
}

@ThemePreviews
@Composable
private fun ExerciseCardTonesPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(WeightrackTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.m),
            ) {
                PreviewCard(CardTone.Primary, "Prensa de piernas", "Cuádriceps · Máquina", "120", "mar 6 oct")
                PreviewCard(CardTone.Secondary, "Jalón al pecho", "Dorsales · Polea", "45", "jue 8 oct")
                PreviewCard(CardTone.Tertiary, "Press de banca", "Pecho · Barra", "60", "3 series hoy")
                PreviewCard(CardTone.Neutral, "Dominadas con un nombre muy largo", "Dorsales · Peso corporal", null, "Sin series", badge = "Archivado")
            }
        }
    }
}
