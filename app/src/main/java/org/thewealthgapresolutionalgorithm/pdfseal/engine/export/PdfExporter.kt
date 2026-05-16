package org.thewealthgapresolutionalgorithm.pdfseal.engine.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
            for (pageIndex in 0 until session.pageCount) {
                val sizePt = session.pageSizePt(pageIndex)
                val wPt = sizePt.width.coerceAtLeast(1f)
                val hPt = sizePt.height.coerceAtLeast(1f)

                val info = PdfDocument.PageInfo.Builder(
                    Math.round(wPt), Math.round(hPt), pageIndex + 1,
                ).create()
                val outPage = out.startPage(info)
                val canvas = outPage.canvas

                val bmp = renderer.renderPage(session, pageIndex, renderScale)
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

                session.editsFor(pageIndex)
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

    private fun drawSignature(canvas: Canvas, s: SignatureEditObject) {
        if (s.typedName.isEmpty()) return
        val r = s.rectPt
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.colorArgb
            textSize = r.height.coerceIn(8f, 200f) * 0.8f
            isFakeBoldText =
                s.style == SignatureEditObject.SignatureStyle.BOLD_HANDWRITTEN
            textSkewX =
                if (s.style == SignatureEditObject.SignatureStyle.ELEGANT_CURSIVE) {
                    -0.25f
                } else {
                    0f
                }
        }
        canvas.save()
        if (s.rotationDeg != 0f) canvas.rotate(s.rotationDeg, r.left, r.top)
        canvas.drawText(s.typedName, r.left, r.top - paint.ascent(), paint)
        canvas.restore()
        // Phase 5 swaps these Paint hacks for bundled OFL signature fonts.
    }
}
