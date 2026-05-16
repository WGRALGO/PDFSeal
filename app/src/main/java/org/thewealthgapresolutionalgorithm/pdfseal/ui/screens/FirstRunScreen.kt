package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy

/**
 * One-time limits screen. Shown on first launch until the user taps
 * "I understand"; the acknowledgement is stored locally (see AppPrefs). The
 * same information stays available from About / Privacy / Licenses.
 */
@Composable
fun FirstRunScreen(onAcknowledge: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PDFSeal", style = MaterialTheme.typography.headlineMedium)
        Text(HonestCopy.ONE_LINER, style = MaterialTheme.typography.bodyMedium)
        Text(
            HonestCopy.FIRST_RUN_LIMITS,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("I understand") }
    }
}
