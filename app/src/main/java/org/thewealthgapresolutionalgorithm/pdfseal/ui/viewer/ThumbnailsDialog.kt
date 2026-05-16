package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Tap a thumbnail to jump to that page. Thumbnails render lazily. */
@Composable
fun ThumbnailsDialog(
    state: PdfViewerState,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pages — ${state.pageCount}") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                items((0 until state.pageCount).toList()) { i ->
                    val bmp by produceState<android.graphics.Bitmap?>(null, i) {
                        value = state.thumbnail(i)
                    }
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { state.goTo(i) }
                                onDismiss()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val b = bmp
                        if (b != null) {
                            Image(
                                bitmap = b.asImageBitmap(),
                                contentDescription = "Page ${i + 1}",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text("…")
                        }
                        Text(
                            "p${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
