package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.CoverReplaceObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject

/**
 * Visual overlay. Renders each edit object with Compose at the SAME geometry
 * and size the exporter ([org.thewealthgapresolutionalgorithm.pdfseal.engine
 * .edit.EditObjectPainter]) uses, so the on-screen preview reflects the
 * exported result. This fixes the old bugs where the overlay drew a fixed
 * tiny label that ignored font size and the signature font.
 *
 * Input (select / move / resize / pan / zoom) is owned by the single gesture
 * handler in ViewerScreen — this layer never consumes pointers.
 *
 * The parent content Box is laid out at exactly `pageWidthPt * scalePxPerPt`
 * pixels. We convert PDF points → dp with: dp = pt * scalePxPerPt / density.
 */
@Composable
fun EditObjectsLayer(
    state: PdfViewerState,
    scalePxPerPt: Float,
    densityPxPerDp: Float,
) {
    fun Float.pt2dp(): Dp = (this * scalePxPerPt / densityPxPerDp).dp
    fun Float.ptPx(): Float = this * scalePxPerPt

    Box(Modifier.fillMaxSize()) {
        state.overlay
            .sortedBy { it.zOrder }
            .forEach { obj ->
                val r = obj.rectPt.normalized()
                val isSel = obj.id == state.selectedId
                Box(
                    modifier = Modifier
                        .offset(x = r.left.pt2dp(), y = r.top.pt2dp())
                        .size(
                            width = (r.right - r.left).pt2dp(),
                            height = (r.bottom - r.top).pt2dp(),
                        )
                        .then(
                            if (obj is CoverReplaceObject) {
                                Modifier.background(Color(obj.fillArgb))
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (isSel) {
                                Modifier.border(2.dp, Color(0xFF1F6FEB))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    when (obj) {
                        is TextEditObject -> {
                            // px text height -> sp (toSp divides by density,
                            // so fontSize on screen == fontSizePt * scale px,
                            // matching the exporter's paint.textSize).
                            val sizeSp =
                                (obj.fontSizePt.ptPx() / densityPxPerDp).sp
                            if (obj.text.isNotEmpty()) {
                                Text(
                                    text = obj.text,
                                    color = Color(obj.colorArgb),
                                    fontSize = sizeSp,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                        }
                        is SignatureEditObject -> {
                            val family = FontFamily(
                                Font(SignatureFonts.fontRes(obj.style)),
                            )
                            // Exporter uses textSize = rectHeight*0.8.
                            val sizeSp =
                                ((r.bottom - r.top).ptPx() * 0.8f /
                                    densityPxPerDp).sp
                            if (obj.typedName.isNotEmpty()) {
                                Text(
                                    text = obj.typedName,
                                    color = Color(obj.colorArgb),
                                    fontFamily = family,
                                    fontSize = sizeSp,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                        }
                        is CoverReplaceObject ->
                            obj.overlayText.forEach { t ->
                                Text(
                                    text = t.text,
                                    color = Color(t.colorArgb),
                                    fontSize =
                                        (t.fontSizePt.ptPx() /
                                            densityPxPerDp).sp,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                    }

                    // Bottom-right resize handle on the selected object.
                    if (isSel) {
                        Box(
                            Modifier
                                .offset(
                                    x = (r.right - r.left).pt2dp() - 9.dp,
                                    y = (r.bottom - r.top).pt2dp() - 9.dp,
                                )
                                .size(18.dp)
                                .background(Color(0xFF1F6FEB)),
                        )
                    }
                }
            }
    }
}
