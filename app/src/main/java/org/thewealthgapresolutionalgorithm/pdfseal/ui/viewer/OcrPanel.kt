package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * OCR current page. Offline Tesseract — no network, no cloud. Recognised text
 * is shown for review; OCR can make mistakes.
 */
@Composable
fun OcrPanel(
    state: PdfViewerState,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ocr = state.lastOcr
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OCR — current page") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Offline OCR (Tesseract). OCR can make mistakes — review " +
                        "before relying on the text.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.busy) {
                    CircularProgressIndicator(Modifier.padding(top = 12.dp))
                }
                if (ocr != null && ocr.pageIndex == state.pageIndex) {
                    Text(
                        "Lines: ${ocr.boxes.size} · mean confidence: " +
                            "${ocr.meanConfidence.toInt()}% · lang ${ocr.language}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        text = ocr.fullText.ifBlank { "(no text recognised)" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 280.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !state.busy, onClick = onRun) {
                Text("Run OCR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
