package org.thewealthgapresolutionalgorithm.pdfseal.engine.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import org.thewealthgapresolutionalgorithm.pdfseal.R
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfPageRenderer
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.CoverReplaceObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.FileAccessManager

/**
 * Exports a **flattened edited copy** of the PDF. The source is never modified.
 *
 * Method: each page is rendered to a bitmap at [exportDpi] and the user's edit
 * objects are painted on top in PDF-point space, then written as a new PDF via
 * [android.graphics.pdf.PdfDocument]. This is an honest WYSIWYG flatten —
 * pages become raster images, so exported text is not selectable. True
 * annotation-level flattening is a later-phase improvement (see ROADMAP).
 *
 * PDF points (1/72 in) map 1:1 to PdfDocument canvas units, so edit-object
 * rects in points are drawn directly.
 */
class PdfExporter(
    private val context: Context,
    private val files: FileAccessManager,
) {
    private val renderer = PdfPageRenderer()

    fun exportCopy(session: PdfDocumentSession, targetUri: Uri): Uri =
        exportFlattened(session, targetUri)

    fun exportFlattened(
        session: PdfDocumentSession,
        targetUri: Uri,
        exportDpi: Int = 200,
    ): Uri {
        val out = PdfDocument()
        val renderScale = exportDpi / 72f
        try {
            // Honor the export plan: order, deletions, extra rotation.
            val order = session.exportOrder.ifEmpty { (0 until session.pageCount).toList() }
            order.forEachIndexed { outIdx, srcIndex ->
                val sizePt = session.pageSizePt(srcIndex)
                val wPt = sizePt.width.coerceAtLeast(1f)
                val hPt = sizePt.height.coerceAtLeast(1f)
                val rot = ((session.extraRotation[srcIndex] ?: 0) % 360 + 360) % 360
                val swap = rot == 90 || rot == 270
                val outW = if (swap) hPt else wPt
                val outH = if (swap) wPt else hPt

                val info = PdfDocument.PageInfo.Builder(
                    Math.round(outW), Math.round(outH), outIdx + 1,
                ).create()
                val outPage = out.startPage(info)
                val canvas = outPage.canvas

                canvas.save()
                when (rot) {
                    90 -> { canvas.translate(outW, 0f); canvas.rotate(90f) }
                    180 -> { canvas.translate(outW, outH); canvas.rotate(180f) }
                    270 -> { canvas.translate(0f, outH); canvas.rotate(270f) }
                }

                val bmp = renderer.renderPage(session, srcIndex, renderScale)
                try {
                    canvas.drawBitmap(
                        bmp,
                        Rect(0, 0, bmp.width, bmp.height),
                        RectF(0f, 0f, wPt, hPt),
                        null,
                    )
                } finally {
                    bmp.recycle()
                }

                session.editsFor(srcIndex)
                    .sortedBy { it.zOrder }
                    .forEach { obj ->
                        when (obj) {
                            is CoverReplaceObject -> {
                                drawFilledRect(canvas, obj.rectPt.run {
                                    RectF(left, top, right, bottom)
                                }, obj.fillArgb)
                                obj.overlayText.forEach { drawText(canvas, it) }
                            }
                            is TextEditObject -> drawText(canvas, obj)
                            is SignatureEditObject -> drawSignature(canvas, obj)
                        }
                    }

                canvas.restore()
                out.finishPage(outPage)
            }

            files.openOutput(targetUri, "wt").use { os -> out.writeTo(os) }
        } finally {
            out.close()
        }
        return targetUri
    }

    private fun drawFilledRect(canvas: Canvas, r: RectF, argb: Int) {
        canvas.drawRect(r, Paint().apply { color = argb; style = Paint.Style.FILL })
    }

    private fun drawText(canvas: Canvas, t: TextEditObject) {
        if (t.text.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.colorArgb
            textSize = t.fontSizePt
            textAlign = when (t.alignment) {
                TextEditObject.TextAlign.LEFT -> Paint.Align.LEFT
                TextEditObject.TextAlign.CENTER -> Paint.Align.CENTER
                TextEditObject.TextAlign.RIGHT -> Paint.Align.RIGHT
            }
        }
        val r = t.rectPt
        val x = when (t.alignment) {
            TextEditObject.TextAlign.LEFT -> r.left
            TextEditObject.TextAlign.CENTER -> (r.left + r.right) / 2f
            TextEditObject.TextAlign.RIGHT -> r.right
        }
        canvas.save()
        if (t.rotationDeg != 0f) {
            canvas.rotate(t.rotationDeg, r.left, r.top)
        }
        // Baseline ≈ top + ascent.
        canvas.drawText(t.text, x, r.top - paint.ascent(), paint)
        canvas.restore()
    }

    private val signatureTypefaces = HashMap<Int, Typeface>()

    private fun signatureFontRes(style: SignatureEditObject.SignatureStyle): Int =
        when (style) {
            SignatureEditObject.SignatureStyle.ELEGANT_CURSIVE -> R.font.great_vibes
            SignatureEditObject.SignatureStyle.BOLD_HANDWRITTEN -> R.font.pacifico
            SignatureEditObject.SignatureStyle.CLEAN_FORMAL_SCRIPT ->
                R.font.pinyon_script
        }

    private fun drawSignature(canvas: Canvas, s: SignatureEditObject) {
        if (s.typedName.isEmpty()) return
        val r = s.rectPt
        val fontRes = signatureFontRes(s.style)
        val tf = signatureTypefaces.getOrPut(fontRes) {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.colorArgb
            typeface = tf
            textSize = r.height.coerceIn(8f, 200f) * 0.8f
        }
        canvas.save()
        if (s.rotationDeg != 0f) canvas.rotate(s.rotationDeg, r.left, r.top)
        canvas.drawText(s.typedName, r.left, r.top - paint.ascent(), paint)
        canvas.restore()
    }
}
