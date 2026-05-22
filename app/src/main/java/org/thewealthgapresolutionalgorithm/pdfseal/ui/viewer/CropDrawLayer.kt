package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import kotlin.math.abs

/**
 * Crop overlay with an adjustable rectangle: drag the four corner handles to
 * resize, drag inside to move. Pre-filled to a box inset from the page edges so
 * there is always something to adjust. Apply hands the rect (in display points)
 * to [PdfViewerState.beginCropFromDrag]; Cancel leaves crop mode.
 */
@Composable
fun CropDrawLayer(
    state: PdfViewerState,
    contentScalePxPerPt: Float,
) {
    val density = LocalDensity.current.density
    val handlePx = 22f * density
    val tol = handlePx * 1.6f

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()

        // Crop rect in layer pixels: l, t, r, b. Default = 8% inset.
        var l by remember(wPx, hPx) { mutableStateOf(wPx * 0.08f) }
        var t by remember(wPx, hPx) { mutableStateOf(hPx * 0.08f) }
        var r by remember(wPx, hPx) { mutableStateOf(wPx * 0.92f) }
        var b by remember(wPx, hPx) { mutableStateOf(hPx * 0.92f) }

        val minSide = 40f * density

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(wPx, hPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val p = down.position
                        // Which handle/body did we grab?
                        val corner = when {
                            near(p, l, t, tol) -> Corner.NW
                            near(p, r, t, tol) -> Corner.NE
                            near(p, l, b, tol) -> Corner.SW
                            near(p, r, b, tol) -> Corner.SE
                            else -> null
                        }
                        val inside = corner == null &&
                            p.x in l..r && p.y in t..b
                        // Always consume so the page underneath never pans/zooms
                        // while cropping. A drag that's neither on a handle nor
                        // inside the box simply does nothing (but is swallowed).
                        val active = corner != null || inside
                        down.consume()
                        do {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.pressed }
                            if (ch != null && ch.positionChanged()) {
                                val d = ch.position - ch.previousPosition
                                if (active) when (corner) {
                                    Corner.NW -> {
                                        l = (l + d.x).coerceIn(0f, r - minSide)
                                        t = (t + d.y).coerceIn(0f, b - minSide)
                                    }
                                    Corner.NE -> {
                                        r = (r + d.x).coerceIn(l + minSide, wPx)
                                        t = (t + d.y).coerceIn(0f, b - minSide)
                                    }
                                    Corner.SW -> {
                                        l = (l + d.x).coerceIn(0f, r - minSide)
                                        b = (b + d.y).coerceIn(t + minSide, hPx)
                                    }
                                    Corner.SE -> {
                                        r = (r + d.x).coerceIn(l + minSide, wPx)
                                        b = (b + d.y).coerceIn(t + minSide, hPx)
                                    }
                                    null -> { // move whole box, clamped
                                        val w = r - l
                                        val h = b - t
                                        val nl = (l + d.x).coerceIn(0f, wPx - w)
                                        val nt = (t + d.y).coerceIn(0f, hPx - h)
                                        l = nl; t = nt; r = nl + w; b = nt + h
                                    }
                                }
                                ch.consume()
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val dim = Color(0x99000000)
                // Dim the four regions outside the crop rect.
                drawRect(dim, topLeft = Offset(0f, 0f), size = Size(size.width, t))
                drawRect(dim, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
                drawRect(dim, topLeft = Offset(0f, t), size = Size(l, b - t))
                drawRect(dim, topLeft = Offset(r, t), size = Size(size.width - r, b - t))
                // Border + corner handles.
                drawRect(
                    Color(0xFF3366FF),
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    style = Stroke(3f),
                )
                listOf(
                    Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b),
                ).forEach { c ->
                    drawCircle(Color(0xFFFFFFFF), handlePx / 2f, c)
                    drawCircle(Color(0xFF3366FF), handlePx / 2f, c, style = Stroke(3f))
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(onClick = { state.cropMode = false }) {
                    Text("Cancel")
                }
                Button(onClick = {
                    state.beginCropFromDrag(
                        PdfRectF(
                            l / contentScalePxPerPt,
                            t / contentScalePxPerPt,
                            r / contentScalePxPerPt,
                            b / contentScalePxPerPt,
                        ).normalized(),
                    )
                }) { Text("Apply crop") }
            }
        }
    }
}

private enum class Corner { NW, NE, SW, SE }

private fun near(p: Offset, x: Float, y: Float, tol: Float): Boolean =
    abs(p.x - x) <= tol && abs(p.y - y) <= tol
