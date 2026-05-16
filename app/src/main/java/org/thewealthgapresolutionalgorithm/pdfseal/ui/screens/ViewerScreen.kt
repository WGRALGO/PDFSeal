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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.PdfViewerState
import org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer.SignatureDialog
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
                    enabled = !state.busy,
                    onClick = { showTextDialog = true },
                ) { Text("Add Text") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { showSignatureDialog = true },
                ) { Text("Signature") }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { state.coverMode = !state.coverMode },
                ) { Text(if (state.coverMode) "Cover…" else "Cover") }
                if (state.selectedId != null) {
                    Button(onClick = { state.deleteSelected() }) { Text("Delete") }
                }
                Button(
                    enabled = !state.busy && state.pageCount > 0,
                    onClick = { exportLauncher.launch("PDFSeal-edited.pdf") },
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
