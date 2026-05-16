package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfEngine

@Composable
fun HomeScreen(
    engine: PdfEngine,
    onOpen: (Uri) -> Unit,
    onAbout: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onOpen(uri) }

    val recent by engine.recentFiles.recent.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("PDFSeal", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Edit · Visual Signature · OCR · Organize — local & offline",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = { picker.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open PDF") }

        TextButton(onClick = onAbout) { Text("About & licenses") }

        if (recent.isNotEmpty()) {
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recent) { rf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(Uri.parse(rf.uri)) },
                    ) {
                        Text(
                            rf.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
    }
}
