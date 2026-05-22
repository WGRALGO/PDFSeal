package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Pages tools. Shows the page plan as thumbnails (tap one to jump to it) and
 * the page actions: Rotate left/right (current page, live), Crop, Add PDF,
 * Delete by range.
 */
@Composable
fun PagesMenuDialog(
    state: PdfViewerState,
    onJump: (Int) -> Unit,
    onRotate: (Int) -> Unit,
    onRerender: () -> Unit,
    onCrop: () -> Unit,
    onClearCrop: () -> Unit,
    onAddPdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteSpec by remember { mutableStateOf("") }
    val plan = state.exportPlan()

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text("Pages — ${plan.size}") },
        text = {
            Column {
                // --- Action buttons ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        enabled = !state.busy,
                        onClick = { onRotate(-90); onDismiss() },
                    ) { Text("⟲ Left") }
                    OutlinedButton(
                        enabled = !state.busy,
                        onClick = { onRotate(90); onDismiss() },
                    ) { Text("Right ⟳") }
                    OutlinedButton(onClick = onCrop) { Text("Crop") }
                    if (state.currentPageCropped) {
                        OutlinedButton(onClick = onClearCrop) { Text("Uncrop") }
                    }
                    OutlinedButton(onClick = onAddPdf) { Text("Add PDF") }
                }
                Text(
                    "Rotate turns the current page (page ${state.planPos + 1}).",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )

                // --- Delete by range ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = deleteSpec,
                        onValueChange = { deleteSpec = it },
                        singleLine = true,
                        label = { Text("Delete pages e.g. 3-7") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        enabled = deleteSpec.isNotBlank() && plan.size > 1,
                        onClick = {
                            val n = state.deletePagesByRange(deleteSpec)
                            deleteSpec = ""
                            if (n > 0) onRerender()
                            state.lastMessage = if (n > 0) {
                                "Removed $n page(s)."
                            } else "No pages matched that range."
                        },
                    ) { Text("Delete") }
                }

                // --- Thumbnails of the current plan ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .padding(top = 8.dp),
                ) {
                    itemsIndexed(plan) { pos, entry ->
                        val src = entry.first
                        val bmp by produceState<android.graphics.Bitmap?>(
                            null, src, state.planVersion,
                        ) { value = state.thumbnail(src) }
                        Column(
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxWidth()
                                .clickable {
                                    onJump(pos)
                                    onDismiss()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val b = bmp
                            if (b != null) {
                                Image(
                                    bitmap = b.asImageBitmap(),
                                    contentDescription = "Page ${pos + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                )
                            } else {
                                Text("…")
                            }
                            Text(
                                "p${pos + 1}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { state.resetPlan() }) { Text("Reset plan") }
        },
    )
}
