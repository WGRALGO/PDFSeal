package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Bookmarks (document outline). Tap an entry to jump to its page. The "+" adds
 * a bookmark for the page currently open. Bookmarks live in the open session;
 * "Save PDF with bookmarks" writes them into a real (non-flattened) PDF copy.
 */
@Composable
fun BookmarksDialog(
    state: PdfViewerState,
    onJump: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var newTitle by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text("Bookmarks") },
        text = {
            Column {
                if (state.bookmarks.isEmpty()) {
                    Text(
                        "No bookmarks yet. Add one for the page you're on.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(state.bookmarks, key = { it.id }) { bm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = bm.pageIndex >= 0) {
                                        onJump(bm.pageIndex)
                                        onDismiss()
                                    }
                                    .padding(
                                        start = (bm.depth * 16).dp,
                                        top = 6.dp,
                                        bottom = 6.dp,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        bm.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        if (bm.pageIndex >= 0) {
                                            "Page ${bm.pageIndex + 1}"
                                        } else "No page target",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                TextButton(
                                    onClick = { state.deleteBookmark(bm.id) },
                                ) { Text("Delete") }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(0.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        label = { Text("Name (page ${state.planPos + 1})") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            state.addBookmark(newTitle)
                            newTitle = ""
                        },
                    ) { Text("Add") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state.bookmarks.isNotEmpty(),
                onClick = onSave,
            ) { Text("Save PDF with bookmarks") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
