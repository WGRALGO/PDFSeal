package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextToolDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, fontSizePt: Float) -> Unit,
    initialText: String = "",
    initialSizePt: Float = 14f,
) {
    var text by remember { mutableStateOf(initialText) }
    var size by remember { mutableFloatStateOf(initialSizePt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                )
                Text(
                    "Font size: ${size.toInt()} pt",
                    modifier = Modifier.padding(top = 12.dp),
                )
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 6f..72f,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { if (text.isNotBlank()) onConfirm(text, size) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
