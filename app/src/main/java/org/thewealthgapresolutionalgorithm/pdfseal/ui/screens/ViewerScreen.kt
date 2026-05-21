package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.CoverDrawLayer
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.EditObjectsLayer
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.GoToPageDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PagesDialog
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PdfViewerState
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.SignatureDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.ThumbnailsDialog
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.TextToolDialog

private enum class GestureMode { PAN, MOVE, RESIZE }

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
    var showSearchPanel by remember { mutableStateOf(false) }
    var showEditTextDialog by remember { mutableStateOf(false) }
    var showPagesDialog by remember { mutableStateOf(false) }
    var showThumbs by remember { mutableStateOf(false) }
    var showGoTo by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var showCoverWarning by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density
    val context = LocalContext.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let {
            snackbar.showSnackbar(it)
            state.lastMessage = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) scope.launch { state.export(uri) } }

    val hasSelection = state.selectedId != null

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
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding()) {
                    if (hasSelection) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.width(4.dp))
                            if (state.selectedTextObject() != null) {
                                FilledTonalButton(
                                    onClick = { showEditTextDialog = true },
                                ) { Text("Edit text") }
                            }
                            FilledTonalButton(
                                onClick = { state.highlightSelected() },
                            ) { Text("Highlight") }
                            FilledTonalButton(
                                onClick = { state.strikethroughSelected() },
                            ) { Text("Strikethrough") }
                            FilledTonalButton(
                                onClick = { state.deleteSelected() },
                            ) { Text("Delete") }
                            OutlinedButton(
                                onClick = { state.selectedId = null },
                            ) { Text("Done") }
                        }
                        HorizontalDivider()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BarButton("Prev",
                            enabled = state.pageIndex > 0 && !state.busy) {
                            scope.launch { state.goTo(state.pageIndex - 1) }
                        }
                        OutlinedButton(
                            enabled = !state.busy && state.pageCount > 0,
                            onClick = { showGoTo = true },
                        ) {
                            Text(
                                if (state.pageCount > 0) {
                                    "${state.pageIndex + 1}/${state.pageCount}"
                                } else "—",
                            )
                        }
                        BarButton("Next",
                            enabled = state.pageIndex < state.pageCount - 1 &&
                                !state.busy) {
                            scope.launch { state.goTo(state.pageIndex + 1) }
                        }
                        BarButton("Pages…",
                            enabled = !state.busy && state.pageCount > 0) {
                            showThumbs = true
                        }
                        BarSeparator()
                        BarButton("Add Text", enabled = !state.busy) {
                            showTextDialog = true
                        }
                        BarButton("Signature",
                            enabled = !state.busy && state.pageCount > 0) {
                            showSignatureDialog = true
                        }
                        BarButton("Search",
                            enabled = !state.busy && state.pageCount > 0) {
                            showSearchPanel = true
                        }
                        BarButton("OCR",
                            enabled = !state.busy && state.pageCount > 0) {
                            scope.launch { state.runOcrCurrent() }
                        }
                        BarButton("Edit",
                            enabled = !state.busy && state.pageCount > 0) {
                            scope.launch { state.makeEditableCopyCurrent() }
                        }
                        BarSeparator()
                        Button(
                            enabled = !state.busy && state.pageCount > 0,
                            onClick = { showExportWarning = true },
                        ) { Text("Export") }
                    }
                }
            }
        },
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .onSizeChanged { viewport = it },
            contentAlignment = Alignment.Center,
        ) {
            val bmp = state.pageBitmap
            if (bmp != null) {
                // Fit the page bitmap into the viewport while preserving the
                // bitmap's aspect ratio. The previous `.size(wDp,hDp)` forced
                // the bitmap's full native dp size, which exceeded the
                // viewport — Compose then letterboxed the Image inside the
                // oversized Box, but EditObjectsLayer kept placing children
                // in Box-coords, so they fell outside the visible page
                // (signature was invisible).
                val pageAspect = bmp.width.toFloat() / bmp.height
                val viewportAspect = maxWidth / maxHeight
                val fitModifier = if (viewportAspect < pageAspect) {
                    Modifier.fillMaxWidth().aspectRatio(pageAspect)
                } else {
                    Modifier.fillMaxHeight().aspectRatio(pageAspect)
                }
                // Actual on-screen dp/pt for this rendered page — drives both
                // EditObjectsLayer positioning and gesture coord mapping so
                // they stay aligned with what the user sees.
                val pageW = state.pageSizePt.width.coerceAtLeast(1f)
                val effectiveBoxDpWidth = if (viewportAspect < pageAspect) {
                    maxWidth.value
                } else {
                    maxHeight.value * pageAspect
                }
                val scale = effectiveBoxDpWidth * density / pageW
                Box(
                    modifier = fitModifier
                        .graphicsLayer(
                            scaleX = state.zoom,
                            scaleY = state.zoom,
                            translationX = state.panX,
                            translationY = state.panY,
                        )
                        .pointerInput(bmp, scale) {
                            val handleTolPt = 22f / scale
                            awaitEachGesture {
                                val down =
                                    awaitFirstDown(requireUnconsumed = false)
                                val sx = down.position.x / scale
                                val sy = down.position.y / scale
                                var corner: PdfViewerState.HandleCorner? = null
                                var startRect: PdfRectF? = null
                                var mode: GestureMode = when {
                                    state.selectedId != null &&
                                        state.hitTestHandle(
                                            sx, sy, handleTolPt,
                                        ).also { corner = it } != null -> {
                                        startRect = state.selectedObject()
                                            ?.rectPt?.normalized()
                                        GestureMode.RESIZE
                                    }
                                    else -> {
                                        val hit = state.hitTest(sx, sy)
                                        if (hit != null) {
                                            state.selectedId = hit
                                            GestureMode.MOVE
                                        } else GestureMode.PAN
                                    }
                                }
                                var moved = false
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount =
                                        event.changes.count { it.pressed }
                                    // Two-finger pinch on a selected object
                                    // promotes MOVE to centre-pinch RESIZE.
                                    // Page zoom is suspended while we own the
                                    // selection.
                                    if (mode == GestureMode.MOVE &&
                                        pressedCount >= 2 &&
                                        state.selectedId != null) {
                                        mode = GestureMode.RESIZE
                                        corner = null
                                        startRect = null
                                    }
                                    when {
                                        mode == GestureMode.PAN -> {
                                            val z = event.calculateZoom()
                                            val p = event.calculatePan()
                                            if (z != 1f ||
                                                p.x != 0f || p.y != 0f) {
                                                moved = true
                                                state.zoom =
                                                    (state.zoom * z)
                                                        .coerceIn(0.5f, 6f)
                                                state.panX += p.x
                                                state.panY += p.y
                                                state.clampPan(
                                                    size.width.toFloat(),
                                                    size.height.toFloat(),
                                                    viewport.width.toFloat(),
                                                    viewport.height.toFloat(),
                                                )
                                            }
                                        }
                                        mode == GestureMode.RESIZE &&
                                            corner != null &&
                                            startRect != null -> {
                                            val pressed = event.changes
                                                .firstOrNull { it.pressed }
                                            if (pressed != null &&
                                                pressed.positionChanged()) {
                                                moved = true
                                                state.resizeSelectedByCorner(
                                                    corner!!, startRect!!,
                                                    pressed.position.x / scale,
                                                    pressed.position.y / scale,
                                                )
                                            }
                                        }
                                        mode == GestureMode.RESIZE -> {
                                            val z = event.calculateZoom()
                                            if (z != 1f) {
                                                moved = true
                                                state.scaleSelectedAroundCenter(
                                                    z,
                                                )
                                            }
                                        }
                                        else /* MOVE */ -> {
                                            val d = event.calculatePan()
                                            if (d.x != 0f || d.y != 0f) {
                                                moved = true
                                                state.moveSelectedByPdf(
                                                    d.x / scale,
                                                    d.y / scale,
                                                )
                                            }
                                        }
                                    }
                                    event.changes.forEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                                if (!moved && mode == GestureMode.PAN) {
                                    state.selectedId = null
                                }
                            }
                        },
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF page",
                        modifier = Modifier.fillMaxSize(),
                    )
                    EditObjectsLayer(
                        state = state,
                        scalePxPerPt = scale,
                        densityPxPerDp = density,
                    )
                    if (state.coverMode) {
                        CoverDrawLayer(
                            state = state,
                            contentScalePxPerPt = scale,
                        )
                    }
                }
            } else if (state.openFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        state.lastMessage
                            ?: "PDFSeal could not open this PDF.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
            } else {
                Text("Loading…", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (showTextDialog) {
        TextToolDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text, sizePt, family, bold, italic ->
                state.addTextCentered(text, sizePt, family, bold, italic)
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
                initialFamily = sel.fontFamily,
                initialBold = sel.bold,
                initialItalic = sel.italic,
                onDismiss = { showEditTextDialog = false },
                onConfirm = { text, sizePt, family, bold, italic ->
                    state.updateSelectedText(
                        text, sizePt, family, bold, italic,
                    )
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

    if (showGoTo) {
        GoToPageDialog(
            pageCount = state.pageCount,
            currentPage1 = state.pageIndex + 1,
            onDismiss = { showGoTo = false },
            onGo = { idx -> scope.launch { state.goTo(idx) } },
        )
    }

    if (showSearchPanel) {
        org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.SearchPanel(
            state = state,
            onIndex = { scope.launch { state.runOcrDocument() } },
            onJumpToPage = { p -> scope.launch { state.goTo(p) } },
            onDismiss = { showSearchPanel = false },
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
            onConfirm = { name, style, colorArgb ->
                state.addSignatureCentered(context, name, style, colorArgb)
                showSignatureDialog = false
            },
        )
    }
}

@Composable
private fun BarButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.width(124.dp),
    ) { Text(label, maxLines = 1) }
}

@Composable
private fun BarSeparator() {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
