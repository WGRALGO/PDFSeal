package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Jump straight to a page number (1-based), validated to 1..pageCount. */
@Composable
fun GoToPageDialog(
    pageCount: Int,
    currentPage1: Int,
    onDismiss: () -> Unit,
    onGo: (zeroBasedIndex: Int) -> Unit,
) {
    var raw by remember { mutableStateOf(currentPage1.toString()) }
    val parsed = raw.toIntOrNull()
    val valid = parsed != null && parsed in 1..pageCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to page") },
        text = {
            Column {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { s -> raw = s.filter { it.isDigit() }.take(6) },
                    label = { Text("Page (1–$pageCount)") },
                    isError = raw.isNotEmpty() && !valid,
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (raw.isNotEmpty() && !valid) {
                    Text(
                        "Enter a number between 1 and $pageCount.",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { if (valid) { onGo(parsed!! - 1); onDismiss() } },
            ) { Text("Go") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
