package org.thewealthgapresolutionalgorithm.pdfseal.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.artifex.mupdf.fitz.Document
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.thewealthgapresolutionalgorithm.pdfseal.engine.export.PdfExporter
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.FileAccessManager
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.RecentFilesManager
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrPageResult
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrService
import java.util.concurrent.Executors

/**
 * The single API the UI uses. The UI must never touch MuPDF directly.
 *
 * MuPDF objects are not thread-safe, so every call into MuPDF is confined to
 * one dedicated thread via [mupdfDispatcher].
 */
class PdfEngine(context: Context) {

    private val appContext = context.applicationContext
    private val files = FileAccessManager(appContext)
    private val renderer = PdfPageRenderer()
    private val ocr = OcrService(appContext)
    private val exporter = PdfExporter(appContext, files)

    val recentFiles = RecentFilesManager(appContext)

    private val mupdfDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "pdfseal-mupdf") }
            .asCoroutineDispatcher()

    suspend fun openDocument(uri: Uri): PdfDocumentSession =
        withContext(mupdfDispatcher) {
            files.takePersistableRead(uri)
            val name = files.displayName(uri)

            // Stream into a private cache temp file; MuPDF opens it by path and
            // memory-maps lazily — the whole PDF is never held in RAM.
            val temp = files.copyToCacheTempFile(uri)

            val doc = try {
                Document.openDocument(temp.absolutePath)
            } catch (e: Throwable) {
                temp.delete()
                throw java.io.IOException(
                    "This PDF could not be opened. It may be corrupt, " +
                        "truncated, or not a supported PDF.",
                    e,
                )
            }

            if (doc.needsPassword()) {
                runCatching { doc.destroy() }
                temp.delete()
                throw java.io.IOException(
                    "This PDF is password-protected. PDFSeal cannot open " +
                        "encrypted PDFs yet — remove the password first.",
                )
            }

            val pages = try {
                doc.countPages()
            } catch (e: Throwable) {
                runCatching { doc.destroy() }
                temp.delete()
                throw java.io.IOException(
                    "This PDF is unreadable or has no pages. It may be corrupt " +
                        "or an unsupported format.",
                    e,
                )
            }
            if (pages <= 0) {
                runCatching { doc.destroy() }
                temp.delete()
                throw java.io.IOException("This PDF has no pages.")
            }

            val session = PdfDocumentSession(uri, name, doc, temp)
            recentFiles.add(uri.toString(), name)
            session
        }

    suspend fun renderPage(
        session: PdfDocumentSession,
        pageIndex: Int,
        renderScale: Float,
    ): Bitmap = withContext(mupdfDispatcher) {
        renderer.renderPage(session, pageIndex, renderScale)
    }

    suspend fun renderThumbnail(
        session: PdfDocumentSession,
        pageIndex: Int,
        maxEdgePx: Int = 256,
    ): Bitmap = withContext(mupdfDispatcher) {
        renderer.renderThumbnail(session, pageIndex, maxEdgePx)
    }

    /** OCR a single page. Renders at [ocrDpi] then runs offline Tesseract. */
    suspend fun ocrPage(
        session: PdfDocumentSession,
        pageIndex: Int,
        ocrDpi: Int = 300,
        language: String = "eng",
    ): OcrPageResult = withContext(mupdfDispatcher) {
        val size = session.pageSizePt(pageIndex)
        val bmp = renderer.renderPage(session, pageIndex, renderer.scaleForDpi(ocrDpi))
        val result = ocr.recognizePage(
            bitmap = bmp,
            pageIndex = pageIndex,
            pdfPageWidthPt = size.width,
            pdfPageHeightPt = size.height,
            language = language,
        )
        session.ocrResults[pageIndex] = result
        result
    }

    suspend fun exportCopy(session: PdfDocumentSession, targetUri: Uri): Uri =
        withContext(mupdfDispatcher) {
            files.takePersistableReadWrite(targetUri)
            exporter.exportCopy(session, targetUri)
        }

    suspend fun closeDocument(session: PdfDocumentSession) =
        withContext(mupdfDispatcher) { session.close() }
}
