package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.EditObjectPainter

/**
 * Purely visual. Draws every edit object through the SAME [EditObjectPainter]
 * the exporter uses, so the on-screen preview is pixel-faithful to the exported
 * PDF (fixes the old "signature/font size doesn't show" bugs — the previous
 * layer drew a fixed Compose label instead).
 *
 * All input (select / move / resize / pan / zoom) is handled by the single
 * gesture handler in ViewerScreen — this layer never consumes pointers.
 *
 * The parent content Box is laid out at exactly `pageWidthPt * scalePxPerPt`
 * pixels, so 1 canvas px == 1 pt * [scalePxPerPt]. We scale the native canvas
 * by that factor and the painter then works in pure PDF points.
 */
@Composable
fun EditObjectsLayer(
    state: PdfViewerState,
    scalePxPerPt: Float,
    handleRadiusPx: Float,
) {
    val context = LocalContext.current
    val painter = remember(context) { EditObjectPainter(context) }
    val accent = Color(0xFF1F6FEB)

    Canvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            state.overlay
                .sortedBy { it.zOrder }
                .forEach { obj ->
                    nc.save()
                    nc.scale(scalePxPerPt, scalePxPerPt)
                    painter.draw(nc, obj)
                    nc.restore()
                }
        }

        // Selection chrome in pixel space.
        val sel = state.overlay.firstOrNull { it.id == state.selectedId }
        if (sel != null) {
            val r = sel.rectPt
            val left = r.left * scalePxPerPt
            val top = r.top * scalePxPerPt
            val w = (r.right - r.left) * scalePxPerPt
            val h = (r.bottom - r.top) * scalePxPerPt
            drawRect(
                color = accent,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = 2f),
            )
            // Bottom-right resize handle.
            drawCircle(
                color = accent,
                radius = handleRadiusPx,
                center = Offset(left + w, top + h),
            )
        }
    }
}
