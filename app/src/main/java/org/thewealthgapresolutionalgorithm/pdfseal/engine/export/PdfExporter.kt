package org.thewealthgapresolutionalgorithm.pdfseal.engine.export

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfPageRenderer
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.EditObjectPainter
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
    private val painter = EditObjectPainter(context)

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
                    .forEach { obj -> painter.draw(canvas, obj) }

                canvas.restore()
                out.finishPage(outPage)
            }

            files.openOutput(targetUri, "wt").use { os -> out.writeTo(os) }
        } finally {
            out.close()
        }
        return targetUri
    }
}
