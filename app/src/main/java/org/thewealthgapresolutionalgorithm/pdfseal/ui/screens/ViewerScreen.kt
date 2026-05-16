package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.CoverDrawLayer
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.EditObjectsLayer
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.OcrPanel
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PagesDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PdfViewerState
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.SignatureDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.ThumbnailsDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.TextToolDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    state: PdfViewerState,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showTextDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showOcrPanel by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var showPagesDialog by remember { mutableStateOf(false) }
    var showThumbs by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var showCoverWarning by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let {
            snackbar.showSnackbar(it)
            state.lastMessage = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) scope.launch { state.export(uri) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.coverMode -> "Drag to cover an area"
                            state.pageCount > 0 ->
                                "Page ${state.pageIndex + 1} / ${state.pageCount}"
                            else -> "PDFSeal"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    enabled = state.pageIndex > 0 && !state.busy,
                    onClick = { scope.launch { state.goTo(state.pageIndex - 1) } },
                ) { Text("Prev") }
                Button(
                    enabled = state.pageIndex < state.pageCount - 1 && !state.busy,
                    onClick = { scope.launch { state.goTo(state.pageIndex + 1) } },
                ) { Text("Next") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showThumbs = true },
                ) { Text("Thumbs") }
                Button(
                    enabled = !state.busy,
                    onClick = { showTextDialog = true },
                ) { Text("Add Text") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showSignatureDialog = true },
                ) { Text("Visual Signature") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = {
                        if (state.coverMode) {
                            state.coverMode = false
                        } else {
                            showCoverWarning = true
                        }
                    },
                ) { Text(if (state.coverMode) "Cover…" else "Cover") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showOcrPanel = true },
                ) { Text("OCR") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showPagesDialog = true },
                ) { Text("Pages") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { scope.launch { state.makeEditableCopyCurrent() } },
                ) { Text("Editable Copy") }
                if (state.selectedTextObject() != null) {
                    Button(onClick = { showEditTextDialog = true }) { Text("Edit") }
                }
                if (state.selectedId != null) {
                    Button(onClick = { state.deleteSelected() }) { Text("Delete") }
                }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showExportWarning = true },
                ) { Text("Export") }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        state.zoom = (state.zoom * zoomChange).coerceIn(0.5f, 6f)
                        state.panX += pan.x
                        state.panY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { state.selectedId = null })
                },
            contentAlignment = Alignment.Center,
        ) {
            val bmp = state.pageBitmap
            if (bmp != null) {
                val wDp = (bmp.width / density).dp
                val hDp = (bmp.height / density).dp
                Box(
                    modifier = Modifier
                        .size(wDp, hDp)
                        .graphicsLayer(
                            scaleX = state.zoom,
                            scaleY = state.zoom,
                            translationX = state.panX,
                            translationY = state.panY,
                        ),
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF page",
                        modifier = Modifier.fillMaxSize(),
                    )
                    EditObjectsLayer(
                        state = state,
                        contentScalePxPerPt = state.renderScale,
                        densityPxPerDp = density,
                    )
                    if (state.coverMode) {
                        CoverDrawLayer(
                            state = state,
                            contentScalePxPerPt = state.renderScale,
                        )
                    }
                }
            } else {
                Text("Loading…", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (showTextDialog) {
        TextToolDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text, sizePt ->
                state.addTextCentered(text, sizePt)
                showTextDialog = false
            },
        )
    }

    if (showEditTextDialog) {
        val sel = state.selectedTextObject()
        if (sel != null) {
            TextToolDialog(
                initialText = sel.text,
                initialSizePt = sel.fontSizePt,
                onDismiss = { showEditTextDialog = false },
                onConfirm = { text, sizePt ->
                    state.updateSelectedText(text, sizePt)
                    showEditTextDialog = false
                },
            )
        } else {
            showEditTextDialog = false
        }
    }

    if (showPagesDialog) {
        PagesDialog(state = state, onDismiss = { showPagesDialog = false })
    }

    if (showThumbs) {
        ThumbnailsDialog(state = state, onDismiss = { showThumbs = false })
    }

    if (showOcrPanel) {
        OcrPanel(
            state = state,
            onRun = { scope.launch { state.runOcrCurrent() } },
            onDismiss = { showOcrPanel = false },
        )
    }

    if (showCoverWarning) {
        AlertDialog(
            onDismissRequest = { showCoverWarning = false },
            title = { Text("Cover & Replace") },
            text = {
                Text(
                    org.thewealthgapresolutionalgorithm.pdfseal.ui
                        .HonestCopy.COVER_WARNING,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCoverWarning = false
                    state.coverMode = true
                }) { Text("I understand — start cover") }
            },
            dismissButton = {
                TextButton(onClick = { showCoverWarning = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showExportWarning) {
        val body =
            org.thewealthgapresolutionalgorithm.pdfseal.ui.HonestCopy
                .EXPORT_CONFIRM +
                if (state.usesCoverReplace()) {
                    "\n\n" + org.thewealthgapresolutionalgorithm.pdfseal.ui
                        .HonestCopy.EXPORT_COVER_NOTICE
                } else {
                    ""
                }
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text("Export a flattened visual PDF copy") },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    showExportWarning = false
                    exportLauncher.launch(state.defaultExportName())
                }) { Text("Export Flattened Copy") }
            },
            dismissButton = {
                TextButton(onClick = { showExportWarning = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSignatureDialog) {
        SignatureDialog(
            onDismiss = { showSignatureDialog = false },
            onConfirm = { name, style ->
                state.addSignatureCentered(name, style)
                showSignatureDialog = false
            },
        )
    }
}
