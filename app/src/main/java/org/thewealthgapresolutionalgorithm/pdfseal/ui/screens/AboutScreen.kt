package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.thewealthgapresolutionalgorithm.pdfseal.BuildConfig

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("PDFSeal", style = MaterialTheme.typography.headlineMedium)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Text("License: AGPL-3.0-or-later")
        Text("Source: ${BuildConfig.SOURCE_URL}")
        Text(
            "Privacy: PDFSeal is local and offline. No cloud upload, no server " +
                "processing, no account, no analytics, no ads, no trackers, no " +
                "Google Play Services.",
        )
        Text(
            "Honest features: Add Text adds new text; Cover & Replace is visual " +
                "only and is NOT secure redaction; Make Editable Copy is OCR-based " +
                "reconstruction, not native PDF text editing. OCR can make " +
                "mistakes — review before relying on it.",
        )
        Text(
            "Third-party: MuPDF (AGPL-3.0), Tesseract4Android + Tesseract " +
                "(Apache-2.0), Leptonica (BSD-2), AndroidX/Compose & Kotlin " +
                "(Apache-2.0), signature fonts (SIL OFL 1.1). Full notices in " +
                "THIRD_PARTY_LICENSES.md.",
        )
        Text(
            "Signing certificate SHA-256:\n${BuildConfig.SIGNING_CERT_SHA256}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
