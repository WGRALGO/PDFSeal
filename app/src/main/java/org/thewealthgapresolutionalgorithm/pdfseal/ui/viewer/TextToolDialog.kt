package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TextToolDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        fontSizePt: Float,
        fontFamily: String,
        bold: Boolean,
        italic: Boolean,
    ) -> Unit,
    initialText: String = "",
    initialSizePt: Float = 14f,
    initialFamily: String = "Sans",
    initialBold: Boolean = false,
    initialItalic: Boolean = false,
) {
    var text by remember { mutableStateOf(initialText) }
    var size by remember { mutableFloatStateOf(initialSizePt) }
    var family by remember { mutableStateOf(initialFamily) }
    var bold by remember { mutableStateOf(initialBold) }
    var italic by remember { mutableStateOf(initialItalic) }

    val previewFamily = when (family) {
        "Serif" -> FontFamily.Serif
        "Mono" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText.isBlank()) "Add text" else "Edit text") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                )
                Text("Font", modifier = Modifier.padding(top = 12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = family == "Sans",
                        onClick = { family = "Sans" },
                        label = { Text("Sans", fontFamily = FontFamily.SansSerif) },
                    )
                    FilterChip(
                        selected = family == "Serif",
                        onClick = { family = "Serif" },
                        label = { Text("Serif", fontFamily = FontFamily.Serif) },
                    )
                    FilterChip(
                        selected = family == "Mono",
                        onClick = { family = "Mono" },
                        label = { Text("Mono", fontFamily = FontFamily.Monospace) },
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = bold,
                        onClick = { bold = !bold },
                        label = {
                            Text("Bold", fontWeight = FontWeight.Bold)
                        },
                    )
                    FilterChip(
                        selected = italic,
                        onClick = { italic = !italic },
                        label = {
                            Text("Italic", fontStyle = FontStyle.Italic)
                        },
                    )
                }
                Text(
                    "Font size: ${size.toInt()} pt",
                    modifier = Modifier.padding(top = 12.dp),
                )
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 6f..72f,
                )
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        fontFamily = previewFamily,
                        fontWeight =
                            if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle =
                            if (italic) FontStyle.Italic else FontStyle.Normal,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text, size, family, bold, italic)
                    }
                },
            ) { Text(if (initialText.isBlank()) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
