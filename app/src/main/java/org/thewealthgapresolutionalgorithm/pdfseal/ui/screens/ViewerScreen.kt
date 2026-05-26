package org.thewealthgapresolutionalgorithm.pdfseal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
    var showBookmarks by remember { mutableStateOf(false) }
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

    LaunchedEffect(state.openEditDialogRequested) {
        if (state.openEditDialogRequested) {
            showEditTextDialog = true
            state.openEditDialogRequested = false
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) scope.launch { state.export(uri) } }

    val saveBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) scope.launch { state.saveWithBookmarks(uri) } }

    val addPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) state.pendingAddPdfUri = uri }

    val hasSelection = state.selectedId != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.coverMode -> "Drag to cover an area"
                            state.cropMode -> "Drag the area to keep, then crop"
                            state.editTapMode -> "Tap text to edit it"
                            state.navCount > 0 ->
                                "Page ${state.planPos + 1} / ${state.navCount}"
                            else -> "PDFSeal"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.cropMode) {
                        TextButton(onClick = { state.cropMode = false }) {
                            Text("Cancel crop")
                        }
                    } else {
                        TextButton(
                            enabled = !state.busy && state.pageCount > 0,
                            onClick = { showBookmarks = true },
                        ) { Text("Bookmarks") }
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
                    // Bottom menu: 2-column vertical-scroll grid showing
                    // 2 rows (4 buttons) at a time. Undo/Redo pinned as the
                    // top row so they're always visible without scrolling.
                    val rows: List<Pair<MenuAction?, MenuAction?>> = buildList {
                        add(
                            MenuAction("Undo", state.canUndo && !state.busy) {
                                state.undo()
                            } to MenuAction("Redo", state.canRedo && !state.busy) {
                                state.redo()
                            },
                        )
                        add(
                            MenuAction("Prev",
                                state.planPos > 0 && !state.busy) {
                                scope.launch {
                                    state.goToForEditing(state.planPos - 1)
                                }
                            } to MenuAction("Next",
                                state.planPos < state.navCount - 1 &&
                                    !state.busy) {
                                scope.launch {
                                    state.goToForEditing(state.planPos + 1)
                                }
                            },
                        )
                        val pageLabel = if (state.navCount > 0) {
                            "Go to (${state.planPos + 1}/${state.navCount})"
                        } else "Go to page"
                        add(
                            MenuAction(pageLabel,
                                !state.busy && state.navCount > 0) {
                                showGoTo = true
                            } to MenuAction("Pages…",
                                !state.busy && state.pageCount > 0) {
                                showThumbs = true
                            },
                        )
                        add(
                            MenuAction("Add Text", !state.busy) {
                                showTextDialog = true
                            } to MenuAction("Signature",
                                !state.busy && state.pageCount > 0) {
                                showSignatureDialog = true
                            },
                        )
                        add(
                            MenuAction("Search",
                                !state.busy && state.pageCount > 0) {
                                showSearchPanel = true
                            } to MenuAction("OCR",
                                !state.busy && state.pageCount > 0) {
                                scope.launch { state.runOcrCurrent() }
                            },
                        )
                        add(
                            MenuAction(
                                if (state.editTapMode) "Done editing"
                                else "Edit",
                                !state.busy && state.pageCount > 0,
                            ) {
                                if (state.editTapMode) state.exitEditMode()
                                else scope.launch { state.enterEditMode() }
                            } to MenuAction("Export",
                                !state.busy && state.pageCount > 0,
                                primary = true) {
                                showExportWarning = true
                            },
                        )
                    }
                    // Fixed 2-row viewport (~120dp) with vertical scroll for
                    // the remaining rows. Each row ~56dp tall incl. padding.
                    val rowHeight = 56.dp
                    val visibleRows = 2
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight * visibleRows + 16.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rows.forEach { (left, right) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GridButton(
                                    action = left,
                                    modifier = Modifier.weight(1f),
                                )
                                GridButton(
                                    action = right,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
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
                            // Handles are drawn 18dp × 18dp OUTSIDE the
                            // selection rect, so the centre of a handle sits
                            // ~9dp out from the corner and the far edge ~18dp
                            // out. Tolerance covers the far edge at any zoom.
                            val handleTolPt = (40f / scale).coerceAtLeast(22f)
                            awaitEachGesture {
                                val down =
                                    awaitFirstDown(requireUnconsumed = false)
                                // Crop/Cover overlays own all input while active —
                                // don't let the page pan/zoom underneath them.
                                if (state.cropMode || state.coverMode) {
                                    return@awaitEachGesture
                                }
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
                                // One undo step per drag — snapshot at gesture
                                // START for MOVE/RESIZE; PAN doesn't touch
                                // edits so it's never recorded.
                                if (mode == GestureMode.MOVE ||
                                    mode == GestureMode.RESIZE) {
                                    state.snapBeforeGesture()
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
                                    // Tap on empty space: in edit mode, turn the
                                    // tapped text line into an editable overlay;
                                    // otherwise just clear the selection.
                                    if (state.editTapMode) {
                                        state.tapToEdit(sx, sy)
                                    } else {
                                        state.selectedId = null
                                    }
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
                    if (state.cropMode) {
                        org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer
                            .CropDrawLayer(
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

    state.pendingAddPdfUri?.let { addUri ->
        AlertDialog(
            onDismissRequest = { state.pendingAddPdfUri = null },
            title = { Text("Where to add these pages?") },
            text = {
                Text(
                    "Insert the picked PDF's pages at the start, after the " +
                        "current page (page ${state.planPos + 1}), or at the end.",
                )
            },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        state.pendingAddPdfUri = null
                        scope.launch { state.addPdf(addUri, 0) }
                    }) { Text("At the start") }
                    TextButton(onClick = {
                        state.pendingAddPdfUri = null
                        scope.launch {
                            state.addPdf(addUri, state.planPositionAfterCurrent())
                        }
                    }) { Text("After current page") }
                    TextButton(onClick = {
                        state.pendingAddPdfUri = null
                        scope.launch { state.addPdf(addUri, state.planSize) }
                    }) { Text("At the end") }
                }
            },
            dismissButton = {
                TextButton(onClick = { state.pendingAddPdfUri = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.pendingCropFrac != null) {
        AlertDialog(
            onDismissRequest = { state.cancelPendingCrop() },
            title = { Text("Apply crop to…") },
            text = {
                Text(
                    "Crop just this page, or apply the same crop to every page?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { state.applyPendingCrop(allPages = false) }
                }) { Text("This page") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch { state.applyPendingCrop(allPages = true) }
                    }) { Text("All pages") }
                    TextButton(onClick = { state.cancelPendingCrop() }) {
                        Text("Cancel")
                    }
                }
            },
        )
    }

    if (showBookmarks) {
        org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.BookmarksDialog(
            state = state,
            onJump = { p -> scope.launch { state.goToSource(p) } },
            onSave = {
                showBookmarks = false
                saveBookmarksLauncher.launch(state.defaultBookmarkSaveName())
            },
            onDismiss = { showBookmarks = false },
        )
    }

    if (showThumbs) {
        org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PagesMenuDialog(
            state = state,
            onJump = { pos -> scope.launch { state.goToPlan(pos) } },
            onRotate = { d -> scope.launch { state.rotateCurrentPage(d) } },
            onRerender = { scope.launch { state.goToPlan(state.planPos) } },
            onCrop = {
                showThumbs = false
                state.selectedId = null
                state.cropMode = true
            },
            onClearCrop = {
                showThumbs = false
                scope.launch { state.clearCropCurrent() }
            },
            onAddPdf = {
                showThumbs = false
                addPdfLauncher.launch(arrayOf("application/pdf"))
            },
            onDismiss = { showThumbs = false },
        )
    }

    if (showGoTo) {
        GoToPageDialog(
            pageCount = state.navCount,
            currentPage1 = state.planPos + 1,
            onDismiss = { showGoTo = false },
            onGo = { idx -> scope.launch { state.goToPlan(idx) } },
        )
    }

    if (showSearchPanel) {
        org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.SearchPanel(
            state = state,
            onIndex = { scope.launch { state.runOcrDocument() } },
            onJumpToPage = { p -> scope.launch { state.goToSource(p) } },
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

/** Description of a single cell in the bottom 2-column grid menu. */
private data class MenuAction(
    val label: String,
    val enabled: Boolean,
    val primary: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun GridButton(
    action: MenuAction?,
    modifier: Modifier = Modifier,
) {
    if (action == null) {
        Spacer(modifier)
        return
    }
    if (action.primary) {
        Button(
            enabled = action.enabled,
            onClick = action.onClick,
            modifier = modifier,
        ) { Text(action.label, maxLines = 1) }
    } else {
        FilledTonalButton(
            enabled = action.enabled,
            onClick = action.onClick,
            modifier = modifier,
        ) { Text(action.label, maxLines = 1) }
    }
}
