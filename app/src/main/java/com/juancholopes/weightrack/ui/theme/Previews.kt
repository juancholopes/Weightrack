package com.juancholopes.weightrack.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/*
 * Visual loop of Spec 41: previews, the theme gallery and the design lab. Preview-only code: nothing
 * here is reused by the app, and sample texts may be written inline because users never see them.
 */

private const val NightMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL

/**
 * Six previews for every `Content` and shared component (S41-R16): light, dark, dynamic color from a red
 * wallpaper, dynamic color from a blue wallpaper in dark mode, the fallback scheme (API 30) and the font
 * at 200 %. Wrap the previewed composable in [WeightrackTheme].
 */
@Preview(name = "1 Light", group = "Theme")
@Preview(name = "2 Dark", group = "Theme", uiMode = NightMode)
@Preview(name = "3 Dynamic red", group = "Theme", wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE)
@Preview(name = "4 Dynamic blue dark", group = "Theme", wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, uiMode = NightMode)
@Preview(name = "5 Fallback API 30", group = "Theme", apiLevel = 30)
@Preview(name = "6 Font 200%", group = "Theme", fontScale = 2f)
annotation class ThemePreviews

/** Every token of the current theme: color roles, type scale, numbers and shapes. */
@Composable
fun ThemeGallery(modifier: Modifier = Modifier) {
    val spacing = WeightrackTheme.spacing
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
            GallerySection("Color roles") { ColorRoles() }
            GallerySection("Type scale (wdth ${WeightrackTheme.direction.textWidth.roundToInt()})") { TypeScale() }
            GallerySection("Numbers") { NumberSamples() }
            GallerySection("Shapes") { ShapeSamples() }
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.s)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun ColorRoles() {
    val c = MaterialTheme.colorScheme
    val pairs = listOf(
        Triple("primary", c.primary, c.onPrimary),
        Triple("primaryContainer", c.primaryContainer, c.onPrimaryContainer),
        Triple("secondary", c.secondary, c.onSecondary),
        Triple("secondaryContainer", c.secondaryContainer, c.onSecondaryContainer),
        Triple("tertiary", c.tertiary, c.onTertiary),
        Triple("tertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer),
        Triple("error", c.error, c.onError),
        Triple("errorContainer", c.errorContainer, c.onErrorContainer),
        Triple("surface", c.surface, c.onSurface),
        Triple("surfaceContainerLow", c.surfaceContainerLow, c.onSurface),
        Triple("surfaceContainer", c.surfaceContainer, c.onSurface),
        Triple("surfaceContainerHighest", c.surfaceContainerHighest, c.onSurfaceVariant),
        Triple("inverseSurface", c.inverseSurface, c.inverseOnSurface),
    )
    Column(verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.xs)) {
        pairs.forEach { (name, container, content) -> ColorSwatch(name, container, content) }
    }
}

@Composable
private fun ColorSwatch(name: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(container, MaterialTheme.shapes.small)
            .padding(horizontal = WeightrackTheme.spacing.m, vertical = WeightrackTheme.spacing.s),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = name, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun TypeScale() {
    val t = MaterialTheme.typography
    val styles = listOf(
        "Display large" to t.displayLarge, "Display medium" to t.displayMedium, "Display small" to t.displaySmall,
        "Headline large" to t.headlineLarge, "Headline medium" to t.headlineMedium,
        "Headline small" to t.headlineSmall, "Title large" to t.titleLarge, "Title medium" to t.titleMedium,
        "Title small" to t.titleSmall, "Body large" to t.bodyLarge, "Body medium" to t.bodyMedium,
        "Body small" to t.bodySmall, "Label large" to t.labelLarge, "Label medium" to t.labelMedium,
        "Label small" to t.labelSmall,
    )
    Column {
        styles.forEach { (name, style) -> Text(text = name, style = style, color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun NumberSamples() {
    val numbers = WeightrackTheme.numbers
    Column(verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.s)) {
        NumberSample("hero", "62,5", numbers.hero)
        NumberSample("card", "140", numbers.card)
        NumberSample("inline", "60 kg × 10", numbers.inline)
    }
}

@Composable
private fun NumberSample(name: String, value: String, style: TextStyle) {
    Column {
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = style, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ShapeSamples() {
    val s = MaterialTheme.shapes
    val shapes = listOf("XS" to s.extraSmall, "S" to s.small, "M" to s.medium, "L" to s.large, "XL" to s.extraLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.s)) {
        shapes.forEach { (name, shape) ->
            Box(
                modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ThemeGalleryPreview() {
    WeightrackTheme {
        ThemeGallery()
    }
}

/**
 * Interactive design lab (S41-R17): pick a direction, light or dark, dynamic color on or off, and tune
 * the Roboto Flex axes of the big numbers (`wdth`, `wght`, `GRAD`) and the `wdth` of the text. Shows the
 * numbers, a card, a slider and the type scale with the tokens of the chosen direction.
 *
 * Open it on the phone with Android Studio "Run Preview", or with `adb` (see Spec 41, "Ciclo visual").
 * Its content is sample-only and is not reused by the app.
 */
@Composable
fun DesignLab(modifier: Modifier = Modifier) {
    val systemDark = isSystemInDarkTheme()
    var preset by remember { mutableStateOf(DefaultDirection) }
    var darkTheme by remember { mutableStateOf(systemDark) }
    var dynamicColor by remember { mutableStateOf(true) }
    var numberWidth by remember(preset) { mutableFloatStateOf(preset.numbers.hero.width) }
    var numberWeight by remember(preset) { mutableFloatStateOf(preset.numbers.hero.weight.toFloat()) }
    var numberGrade by remember(preset) { mutableFloatStateOf(preset.numbers.hero.grade.toFloat()) }
    var textWidth by remember(preset) { mutableFloatStateOf(preset.textWidth) }

    val direction = remember(preset, numberWidth, numberWeight, numberGrade, textWidth) {
        fun NumberAxis.tuned() = copy(width = numberWidth, weight = numberWeight.roundToInt(), grade = numberGrade.roundToInt())
        preset.copy(
            textWidth = textWidth,
            numbers = preset.numbers.copy(hero = preset.numbers.hero.tuned(), card = preset.numbers.card.tuned()),
        )
    }

    WeightrackTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, direction = direction) {
        val spacing = WeightrackTheme.spacing
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(spacing.l),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
            ) {
                Text(text = "Design lab", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    VisualDirection.presets.forEach { option ->
                        FilterChip(
                            selected = option.name == preset.name,
                            onClick = { preset = option },
                            label = { Text(option.name) },
                        )
                    }
                }
                LabSwitch(label = "Dark", checked = darkTheme, onCheckedChange = { darkTheme = it })
                LabSwitch(label = "Dynamic color", checked = dynamicColor, onCheckedChange = { dynamicColor = it })
                LabSlider("Numbers wdth", numberWidth, RobotoFlexAxes.Width, step = 1f) { numberWidth = it }
                LabSlider("Numbers wght", numberWeight, RobotoFlexAxes.Weight.toFloatRange(), step = 10f) {
                    numberWeight = it
                }
                LabSlider("Numbers GRAD", numberGrade, RobotoFlexAxes.Grade.toFloatRange(), step = 10f) {
                    numberGrade = it
                }
                LabSlider("Text wdth", textWidth, RobotoFlexAxes.Width, step = 1f) { textWidth = it }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    Text(text = "62,5", style = WeightrackTheme.numbers.hero)
                    Text(text = "kg", style = MaterialTheme.typography.titleLarge)
                }
                SampleCard()
                SampleSlider()
                TypeScale()
            }
        }
    }
}

private fun IntRange.toFloatRange(): ClosedFloatingPointRange<Float> = first.toFloat()..last.toFloat()

@Composable
private fun LabSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Slider for one axis; the value is rounded with [quantizeAxis] so the lab creates few typefaces (S41-R08). */
@Composable
private fun LabSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(text = value.roundToInt().toString(), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(quantizeAxis(it, step).coerceIn(range)) },
            valueRange = range,
        )
    }
}

/** A sample exercise card, colored the way the direction says ([CardStyle]). */
@Composable
private fun SampleCard() {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    when (WeightrackTheme.direction.cardStyle) {
        CardStyle.Tonal -> Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
        ) { SampleCardContent() }

        CardStyle.AccentEdge -> Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurface,
            ),
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(colors.primary))
                SampleCardContent()
            }
        }

        CardStyle.Outlined -> OutlinedCard(
            shape = shape,
            colors = CardDefaults.outlinedCardColors(containerColor = colors.surface, contentColor = colors.onSurface),
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) { SampleCardContent() }
    }
}

@Composable
private fun SampleCardContent() {
    val spacing = WeightrackTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(text = "Press de banca", style = MaterialTheme.typography.titleLarge)
        Text(text = "Barra · Pecho", style = MaterialTheme.typography.bodyMedium)
        Text(text = "62,5 kg", style = WeightrackTheme.numbers.card)
    }
}

/** The Material 3 slider of the owner's reference: thick track, bar handle, gap and stop marks. */
@Composable
private fun SampleSlider() {
    var value by remember { mutableFloatStateOf(0.6f) }
    Slider(value = value, onValueChange = { value = it }, steps = 9)
}

@Preview(name = "Design lab", heightDp = 2000)
@Composable
fun DesignLabPreview() {
    DesignLab()
}
