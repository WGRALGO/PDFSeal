package org.thewealthgapresolutionalgorithm.pdfseal.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import com.artifex.mupdf.fitz.Buffer
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.Rect
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfPageRenderer
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.EditObjectPainter
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.FileAccessManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Exports a **flattened edited copy** of the PDF. The source is never modified.
 *
 * Each page is rendered to a bitmap at [exportDpi], the user's edit objects are
 * painted on top in PDF-point space, and the composite is JPEG-encoded and
 * embedded as a `DCTDecode` image XObject. Because the bytes are stored
 * **already compressed**, MuPDF never holds an uncompressed pixmap per page, so
 * memory stays at the compressed size (tens of MB) even for hundreds of pages.
 * Earlier approaches (android.graphics.pdf.PdfDocument and decoded MuPDF images)
 * buffered every page raw in RAM and were killed by the OS — producing a
 * 0-byte file.
 *
 * The PDF is written to a private cache file, verified (`%PDF-` + non-empty),
 * then copied to the SAF destination via ContentResolver. Nothing is reported
 * as success until that succeeds.
 *
 * This is an honest WYSIWYG flatten — pages become raster images, so exported
 * text is not selectable. Order/deletion/rotation/crop come from the export plan.
 */
class PdfExporter(
    private val context: Context,
    private val files: FileAccessManager,
) {
    private val renderer = PdfPageRenderer()
    private val painter = EditObjectPainter(context)

    companion object {
        private const val TAG = "PdfSealExport"
    }

    fun exportCopy(session: PdfDocumentSession, targetUri: Uri): Uri =
        exportFlattened(session, targetUri)

    fun exportFlattened(
        session: PdfDocumentSession,
        targetUri: Uri,
        exportDpi: Int = 150,
        jpegQuality: Int = 85,
    ): Uri {
        val renderScale = exportDpi / 72f
        val order = session.exportOrder.ifEmpty { (0 until session.pageCount).toList() }
        Log.d(TAG, "export start: source=${session.sourceUri} dest=$targetUri " +
            "pages=${order.size} dpi=$exportDpi")

        val dir = File(context.cacheDir, "pdfseal_export").apply { mkdirs() }
        val temp = File.createTempFile("export_", ".pdf", dir)

        try {
            buildPdf(session, order, renderScale, jpegQuality, temp)

            // Verify the file we just built before touching the destination.
            if (temp.length() <= 0L) {
                throw IOException("Export produced an empty file.")
            }
            val head = temp.inputStream().use { ins ->
                val b = ByteArray(5); val n = ins.read(b); if (n < 5) ByteArray(0) else b
            }
            if (!head.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) {
                throw IOException("Export is not a valid PDF (bad header).")
            }
            Log.d(TAG, "temp built ok: ${temp.length()} bytes, header=%PDF-")

            // Copy the verified PDF to the SAF destination via ContentResolver.
            files.takePersistableReadWrite(targetUri)
            var written = 0L
            files.openOutput(targetUri, "wt").use { os ->
                temp.inputStream().use { ins -> written = ins.copyTo(os, 64 * 1024) }
                os.flush()
            }
            Log.d(TAG, "copied $written bytes to destination")

            // Verify the destination actually starts with %PDF- and is non-empty.
            val destHead = files.readLeadingBytes(targetUri, 5)
            if (written <= 0L ||
                !destHead.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))
            ) {
                throw IOException("Destination file failed verification.")
            }
            Log.d(TAG, "export verified at destination")
            return targetUri
        } catch (e: Throwable) {
            Log.e(TAG, "export failed", e)
            // Don't leave a half-written/empty file claiming success.
            files.deleteDocument(targetUri)
            throw e
        } finally {
            runCatching { temp.delete() }
        }
    }

    /** Build the flattened PDF into [temp] using compressed-JPEG image pages. */
    private fun buildPdf(
        session: PdfDocumentSession,
        order: List<Int>,
        renderScale: Float,
        jpegQuality: Int,
        temp: File,
    ) {
        val out = PDFDocument()
        try {
            var pageNo = 0
            order.forEach { srcIndex ->
                val rot = session.rotationOf(srcIndex)
                val crop = session.cropOf(srcIndex)
                val sizePt = session.displayPageSizePt(srcIndex)
                val outW = sizePt.width.coerceAtLeast(1f)
                val outH = sizePt.height.coerceAtLeast(1f)

                // Rotation/crop baked into the upright bitmap; overlays on top.
                val bmp = renderer.renderPage(session, srcIndex, renderScale, rot, crop)
                val pxW = bmp.width
                val pxH = bmp.height
                val jpeg = try {
                    val canvas = Canvas(bmp)
                    canvas.save()
                    canvas.scale(renderScale, renderScale)
                    session.editsFor(srcIndex)
                        .sortedBy { it.zOrder }
                        .forEach { obj -> painter.draw(canvas, obj) }
                    canvas.restore()
                    ByteArrayOutputStream().use { baos ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                        baos.toByteArray()
                    }
                } finally {
                    bmp.recycle()
                }

                // Embed the JPEG bytes raw as a DCTDecode image XObject so MuPDF
                // stores them compressed (no per-page uncompressed pixmap).
                val buffer = Buffer()
                buffer.writeBytes(jpeg)
                val imgDict = out.newDictionary().apply {
                    put("Type", out.newName("XObject"))
                    put("Subtype", out.newName("Image"))
                    put("Width", pxW)
                    put("Height", pxH)
                    put("ColorSpace", out.newName("DeviceRGB"))
                    put("BitsPerComponent", 8)
                    put("Filter", out.newName("DCTDecode"))
                }
                val imgRef = out.addRawStream(buffer, imgDict)
                buffer.destroy()

                val xobjs = out.newDictionary()
                xobjs.put("Im0", imgRef)
                val resources = out.newDictionary()
                resources.put("XObject", xobjs)
                val contents = String.format(
                    Locale.US, "q %.2f 0 0 %.2f 0 0 cm /Im0 Do Q", outW, outH,
                )
                val page = out.addPage(Rect(0f, 0f, outW, outH), 0, resources, contents)
                out.insertPage(-1, page)
                pageNo++
                if (pageNo % 25 == 0) Log.d(TAG, "flattened $pageNo/${order.size} pages")
            }
            Log.d(TAG, "all $pageNo pages added; saving…")
            out.save(temp.absolutePath, "")
            Log.d(TAG, "saved temp ${temp.length()} bytes")
        } finally {
            runCatching { out.destroy() }
        }
    }
}
