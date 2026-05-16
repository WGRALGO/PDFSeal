package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject.SignatureStyle

/**
 * Typed-name signature. NOT a certified/cryptographic signature — a visual
 * typed-name stamp the user places and the exporter flattens.
 */
@Composable
fun SignatureDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, style: SignatureStyle) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(SignatureStyle.ELEGANT_CURSIVE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add signature") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Visual typed-name signature — not a certified signature.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                )
                SignatureStyle.entries.forEach { s ->
                    val selected = s == style
                    val family = FontFamily(Font(SignatureFonts.fontRes(s)))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (selected) Color(0x223366FF) else Color.Transparent,
                            )
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0x55888888)
                                },
                            )
                            .clickable { style = s }
                            .padding(10.dp),
                    ) {
                        Text(
                            text = name.ifBlank { "Your Name" },
                            fontFamily = family,
                            fontSize = 30.sp,
                        )
                        Text(
                            SignatureFonts.label(s),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, style) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
