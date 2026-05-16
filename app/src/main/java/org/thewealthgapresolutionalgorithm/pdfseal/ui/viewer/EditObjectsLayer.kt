package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject

/**
 * Draws edit objects in content space (renderScale px per PDF point). The
 * parent applies zoom/pan via graphicsLayer, so this layer always matches the
 * page image. Drag moves the selected object; deltas are converted back to PDF
 * points so geometry stays authoritative.
 */
@Composable
fun EditObjectsLayer(
    state: PdfViewerState,
    contentScalePxPerPt: Float,
    densityPxPerDp: Float,
) {
    fun Float.pt2dp(): Dp = (this * contentScalePxPerPt / densityPxPerDp).dp

    Box(Modifier.fillMaxSize()) {
        state.overlay.forEach { obj ->
            val r = obj.rectPt
            val isSel = obj.id == state.selectedId
            val label = when (obj) {
                is TextEditObject -> obj.text.ifEmpty { "text" }
                is SignatureEditObject -> obj.typedName.ifEmpty { "signature" }
                else -> "object"
            }
            Box(
                modifier = Modifier
                    .offset(x = r.left.pt2dp(), y = r.top.pt2dp())
                    .size(
                        width = (r.right - r.left).pt2dp(),
                        height = (r.bottom - r.top).pt2dp(),
                    )
                    .background(
                        if (isSel) Color(0x223366FF) else Color.Transparent,
                    )
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0x55888888)
                        },
                    )
                    .pointerInput(obj.id, contentScalePxPerPt, state.zoom) {
                        detectDragGestures(
                            onDragStart = { state.selectedId = obj.id },
                        ) { change, drag ->
                            change.consume()
                            // screen px -> content px (/zoom) -> pt (/scale)
                            val dxPt = drag.x / state.zoom / contentScalePxPerPt
                            val dyPt = drag.y / state.zoom / contentScalePxPerPt
                            state.moveSelectedByPdf(dxPt, dyPt)
                        }
                    },
            ) {
                Text(
                    text = label,
                    color = Color(0xFF111111),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
