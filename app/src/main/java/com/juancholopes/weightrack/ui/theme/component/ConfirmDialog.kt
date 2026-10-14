package com.juancholopes.weightrack.ui.theme.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme

/**
 * Confirmation dialog (Spec 41 "Patrones de la Spec 40"). Stateless: whether it is open is the caller's
 * `UiState`.
 *
 * @param title the action as a question, such as "¿Eliminar esta serie?".
 * @param text the consequence, such as "No se puede deshacer.".
 * @param confirmLabel the action verb, such as "Eliminar serie".
 * @param dismissLabel the alternative, such as "Conservar"; never "Cancelar" alone.
 * @param isDestructive paints the confirm button with the `error` color.
 * @param onDismiss back, a tap outside or the dismiss button.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val confirmColors = if (isDestructive) {
        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.textButtonColors()
    }
    // Material 3 buttons already measure at least 48 dp where they are touched (S40-R46).
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = title, modifier = Modifier.semantics { heading() }) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = confirmColors) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@ThemePreviews
@Composable
private fun ConfirmDialogDestructivePreview() {
    WeightrackTheme {
        ConfirmDialog(
            title = "¿Eliminar «Remo con mancuerna»?",
            text = "No tiene series, así que se borra para siempre.",
            confirmLabel = "Eliminar ejercicio",
            dismissLabel = "Conservar",
            onConfirm = {},
            onDismiss = {},
            isDestructive = true,
        )
    }
}

@ThemePreviews
@Composable
private fun ConfirmDialogNeutralPreview() {
    WeightrackTheme {
        ConfirmDialog(
            title = "¿Descartar los cambios?",
            text = "Lo que cambiaste no se ha guardado.",
            confirmLabel = "Descartar",
            dismissLabel = "Seguir editando",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
