package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Page tools — operate on the export plan only. The viewer keeps showing the
 * original pages; changes here apply when you Export.
 *
 * Split = delete the pages you don't want, then Export. Merge across files is
 * not yet available (see ROADMAP).
 */
@Composable
fun PagesDialog(
    state: PdfViewerState,
    onDismiss: () -> Unit,
) {
    val plan = state.exportPlan()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pages — export plan") },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                itemsIndexed(plan) { pos, (src, rot) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "#${pos + 1}  src p${src + 1}  ${rot}°",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.34f),
                        )
                        TextButton(onClick = { state.rotatePagePlan(src) }) {
                            Text("⟳90")
                        }
                        TextButton(
                            enabled = pos > 0,
                            onClick = { state.movePagePlan(pos, pos - 1) },
                        ) { Text("↑") }
                        TextButton(
                            enabled = pos < plan.size - 1,
                            onClick = { state.movePagePlan(pos, pos + 1) },
                        ) { Text("↓") }
                        TextButton(
                            enabled = plan.size > 1,
                            onClick = { state.deletePagePlan(src) },
                        ) { Text("Del") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { state.resetPlan() }) { Text("Reset") }
        },
    )
}
