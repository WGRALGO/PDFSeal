package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class Hit(
    val pageIndex: Int,
    val charOffset: Int,
    val snippet: AnnotatedString,
)

/**
 * Top-level Search. Finds [query] across the document. Uses the OCR text
 * cache if it exists; otherwise prompts the user to "Index document" which
 * runs full-document OCR once. Independent of the OCR panel — never shows
 * OCR semantics (confidence, line counts, raw OCR text).
 */
@Composable
fun SearchPanel(
    state: PdfViewerState,
    onIndex: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val indexed = state.lastOcrAll
    val hasIndex = indexed.isNotEmpty()

    val hits = remember(query, indexed.size) {
        if (query.isBlank() || !hasIndex) emptyList()
        else findHits(indexed, query)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (!hasIndex && !state.busy) {
                    Text(
                        "The document has not been indexed for search yet. " +
                            "Indexing runs offline on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            4.dp, Alignment.End,
                        ),
                    ) {
                        TextButton(onClick = onIndex) { Text("Index document") }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                    return@Column
                }

                if (state.busy && state.ocrProgressPage > 0) {
                    Text(
                        "Indexing page ${state.ocrProgressPage} of " +
                            "${state.ocrProgressTotal}…",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    CircularProgressIndicator(Modifier.padding(top = 6.dp))
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search the document") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                if (query.isNotBlank()) {
                    Text(
                        "${hits.size} match${if (hits.size == 1) "" else "es"}" +
                            " across ${indexed.size} indexed page" +
                            if (indexed.size == 1) "" else "s",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (hits.isNotEmpty()) {
                    Column(
                        Modifier
                            .padding(top = 10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        hits.forEach { hit ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onJumpToPage(hit.pageIndex)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    "p${hit.pageIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium
                                        .copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                                Text(
                                    hit.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        4.dp, Alignment.End,
                    ),
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

private fun findHits(
    indexed: List<org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr
    .OcrPageResult>,
    query: String,
): List<Hit> {
    val out = ArrayList<Hit>()
    val needle = query
    indexed.forEach { page ->
        val text = page.fullText
        var idx = 0
        while (idx < text.length) {
            val next = text.indexOf(needle, idx, ignoreCase = true)
            if (next < 0) break
            out.add(
                Hit(
                    pageIndex = page.pageIndex,
                    charOffset = next,
                    snippet = snippetAround(text, next, needle.length),
                ),
            )
            idx = next + needle.length
        }
    }
    return out
}

private fun snippetAround(
    text: String,
    matchStart: Int,
    matchLen: Int,
    pad: Int = 40,
): AnnotatedString {
    val from = (matchStart - pad).coerceAtLeast(0)
    val to = (matchStart + matchLen + pad).coerceAtMost(text.length)
    val pre = (if (from > 0) "…" else "") + text.substring(from, matchStart)
    val hit = text.substring(matchStart, matchStart + matchLen)
    val post = text.substring(matchStart + matchLen, to) +
        (if (to < text.length) "…" else "")
    return buildAnnotatedString {
        append(pre.replace('\n', ' '))
        withStyle(
            SpanStyle(
                background = androidx.compose.ui.graphics.Color(0xFFFFEB3B),
                fontWeight = FontWeight.Bold,
            ),
        ) { append(hit) }
        append(post.replace('\n', ' '))
    }
}
