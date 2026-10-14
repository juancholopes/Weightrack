package com.juancholopes.weightrack.ui.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme

/** The useful next step an [EmptyState] offers, such as "Crear ejercicio". */
data class EmptyStateAction(val label: String, val onClick: () -> Unit)

/**
 * Empty state of a view: what it is, why it is empty and, if there is one, the useful next action
 * (Spec 41 "Patrones de la Spec 40"). Stateless. A screen shows it only after its first data (S42-R25).
 * The text is centered in the space [modifier] gives it.
 */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    action: EmptyStateAction? = null,
) {
    val spacing = WeightrackTheme.spacing
    Column(
        modifier = modifier.padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Material 3 buttons already measure at least 48 dp where they are touched (S40-R46).
        action?.let { FilledTonalButton(onClick = it.onClick) { Text(it.label) } }
    }
}

@ThemePreviews
@Composable
private fun EmptyStateWithActionPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                text = "Aún no tienes ejercicios. Crea uno para registrar sus series.",
                modifier = Modifier.fillMaxSize(),
                action = EmptyStateAction(label = "Crear ejercicio", onClick = {}),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyStateWithoutActionPreview() {
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                text = "No tienes ejercicios archivados. Los que archives aparecerán aquí.",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
