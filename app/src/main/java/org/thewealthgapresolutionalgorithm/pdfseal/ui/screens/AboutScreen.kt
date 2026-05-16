package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.thewealthgapresolutionalgorithm.pdfseal.BuildConfig

/**
 * About / Privacy / Licenses.
 *
 * Honest, non-misleading copy only. Third-party notices are read from the
 * bundled asset THIRD_PARTY_LICENSES.md (copied from the repo root at build
 * time — see app/build.gradle.kts), so what the user reads is exactly what
 * ships in source.
 */
@Composable
fun AboutScreen() {
    val context = LocalContext.current

    val licenses by produceState(initialValue = "Loading third-party notices…") {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("THIRD_PARTY_LICENSES.md")
                    .bufferedReader().use { it.readText() }
            }.getOrElse {
                "THIRD_PARTY_LICENSES.md could not be read from the app " +
                    "bundle: ${it.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("PDFSeal", style = MaterialTheme.typography.headlineMedium)
        Text("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
        Text("License: AGPL-3.0-or-later")
        Text("Source code: ${BuildConfig.SOURCE_URL}")
        Text(
            "PDFSeal links AGPL-licensed MuPDF, so it is NOT closed source. " +
                "When this app is distributed publicly, the complete " +
                "corresponding source code for this build stays publicly " +
                "available at the URL above (AGPL-3.0 requirement).",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Full license texts are bundled inside this app: AGPL-3.0, " +
                "Apache-2.0 and the Leptonica BSD-2-Clause text under " +
                "assets/licenses/, and the SIL OFL 1.1 font licenses under " +
                "assets/fonts_licenses/. The notices below come from the " +
                "bundled THIRD_PARTY_LICENSES.md.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Credits", style = MaterialTheme.typography.titleMedium)
        Text(
            "PDFSeal v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy.CREDITS_BY)
        Text(
            org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy
                .CREDITS_NO_DATA,
        )
        Text(
            org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy
                .CONTRIBUTORS,
        )
        Text(
            "Owned and maintained by WGRALGO / The Wealth Gap Resolution " +
                "Algorithm Inc. AI tools (Claude) assisted with development " +
                "only and hold no ownership.",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("What PDFSeal is", style = MaterialTheme.typography.titleMedium)
        Text(org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy.ONE_LINER)
        Text(
            org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy
                .FIRST_RUN_LIMITS,
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        Text("Privacy & offline", style = MaterialTheme.typography.titleMedium)
        Text("• PDFSeal works fully offline.")
        Text("• PDFSeal does NOT request the Internet permission.")
        Text("• PDFSeal does NOT upload your PDFs anywhere.")
        Text(
            "• No ads, no analytics, no trackers, no billing, no cloud sync, " +
                "no account, no Google Play Services.",
        )
        Text(
            "• PDFSeal may temporarily process document data locally on this " +
                "device while opening, OCRing, editing and exporting files. " +
                "That processing never leaves the device.",
        )

        HorizontalDivider()

        Text("Honest feature limits", style = MaterialTheme.typography.titleMedium)
        Text(
            "• Export creates a FLATTENED VISUAL PDF copy, not a full " +
                "native-object PDF edit. Original PDF objects (forms, links, " +
                "bookmarks, layers, annotations, selectable text, accessibility " +
                "structure, metadata, existing digital signatures) may not be " +
                "preserved.",
        )
        Text(
            "• \"Visual Signature\" is a typed/drawn visual mark only. It is " +
                "NOT a certified cryptographic digital signature and has no " +
                "legal signing guarantee.",
        )
        Text(
            "• Cover & Replace is a visual cover only — NOT secure redaction. " +
                "Make Editable Copy is OCR reconstruction, not native text " +
                "editing. OCR can make mistakes; review before relying on it.",
        )

        HorizontalDivider()

        Text(
            "Third-party software notices",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(licenses, style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text(
            "Signing certificate SHA-256:\n${BuildConfig.SIGNING_CERT_SHA256}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
