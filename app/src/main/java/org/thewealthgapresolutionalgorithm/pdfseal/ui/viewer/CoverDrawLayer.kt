package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF

/**
 * Active only in Cover & Replace draw mode. Captures a drag rectangle in
 * content-pixel space and converts it to PDF points (px / renderScale). This
 * is a VISUAL cover — never secure redaction.
 */
@Composable
fun CoverDrawLayer(
    state: PdfViewerState,
    contentScalePxPerPt: Float,
) {
    var start by remember { mutableStateOf<Offset?>(null) }
    var current by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x14000000))
            .pointerInput(contentScalePxPerPt) {
                detectDragGestures(
                    onDragStart = { o -> start = o; current = o },
                    onDrag = { change, _ ->
                        change.consume()
                        current = change.position
                    },
                    onDragEnd = {
                        val s = start
                        val c = current
                        if (s != null && c != null) {
                            state.addCover(
                                PdfRectF(
                                    s.x / contentScalePxPerPt,
                                    s.y / contentScalePxPerPt,
                                    c.x / contentScalePxPerPt,
                                    c.y / contentScalePxPerPt,
                                ).normalized(),
                            )
                        }
                        start = null; current = null
                    },
                    onDragCancel = { start = null; current = null },
                )
            },
    ) {
        val s = start
        val c = current
        if (s != null && c != null) {
            Canvas(Modifier.fillMaxSize()) {
                val tl = Offset(minOf(s.x, c.x), minOf(s.y, c.y))
                val sz = Size(kotlin.math.abs(c.x - s.x), kotlin.math.abs(c.y - s.y))
                drawRect(Color(0x55FFFFFF), topLeft = tl, size = sz)
                drawRect(Color(0xFF3366FF), topLeft = tl, size = sz, style = Stroke(2f))
            }
        }
    }
}
