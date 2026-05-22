package org.thewealthgapresolutionalgorithm.pdfseal.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.PDFDocument
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

            // Clear temp PDFs orphaned by a previous crash/kill before opening.
            files.purgeStaleTempFiles()

            // Stream into a private cache temp file; MuPDF opens it by path and
            // memory-maps lazily — the whole PDF is never held in RAM.
            val temp = files.copyToCacheTempFile(uri)

            val doc = try {
                Document.openDocument(temp.absolutePath)
            } catch (e: Throwable) {
                temp.delete()
                throw java.io.IOException(
                    "PDFSeal could not open this PDF. The file may be " +
                        "damaged, encrypted, or unsupported.",
                    e,
                )
            }

            if (doc.needsPassword()) {
                runCatching { doc.destroy() }
                temp.delete()
                throw java.io.IOException(
                    "This PDF is password-protected or encrypted. PDFSeal " +
                        "cannot open encrypted PDFs yet.",
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
        extraRotationDeg: Int = 0,
        cropFrac: PdfRectF? = null,
    ): Bitmap = withContext(mupdfDispatcher) {
        renderer.renderPage(session, pageIndex, renderScale, extraRotationDeg, cropFrac)
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
        val rot = session.rotationOf(pageIndex)
        val size = session.displayPageSizePt(pageIndex)
        val bmp = renderer.renderPage(
            session, pageIndex, renderer.scaleForDpi(ocrDpi), rot,
            session.cropOf(pageIndex),
        )
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

    /**
     * Read the PDF's outline (bookmarks) into a flat, depth-tagged list. Each
     * entry's target is resolved to a 0-based page index via the document's
     * link resolver; entries that don't point at a page (external links, etc.)
     * get pageIndex -1. Cached on the session so we only walk the outline once.
     */
    suspend fun loadBookmarks(session: PdfDocumentSession): List<Bookmark> =
        withContext(mupdfDispatcher) {
            if (session.bookmarksLoaded) return@withContext session.bookmarks.toList()
            val doc = session.document
            val out: Array<Outline>? = runCatching { doc.loadOutline() }.getOrNull()
            val flat = mutableListOf<Bookmark>()
            fun walk(items: Array<Outline>?, depth: Int) {
                items?.forEach { o ->
                    val page = runCatching {
                        if (!o.uri.isNullOrBlank()) {
                            doc.pageNumberFromLocation(doc.resolveLink(o))
                        } else -1
                    }.getOrDefault(-1)
                    flat.add(
                        Bookmark(
                            title = o.title?.takeIf { it.isNotBlank() }
                                ?: "(untitled)",
                            pageIndex = page,
                            depth = depth,
                        ),
                    )
                    walk(o.down, depth + 1)
                }
            }
            walk(out, 0)
            session.bookmarks.clear()
            session.bookmarks.addAll(flat)
            session.bookmarksLoaded = true
            session.bookmarksDirty = false
            flat.toList()
        }

    /**
     * Write a real PDF (not a raster flatten) whose outline is rebuilt from
     * [bookmarks], preserving the source document's pages, text, and links.
     *
     * This is a separate path from [exportCopy]: it does NOT bake in the user's
     * visual overlay edits (added text, signatures, covers) — those still go
     * through the flatten Export. Nesting is preserved using the outline
     * iterator's depth navigation.
     */
    suspend fun saveWithBookmarks(
        session: PdfDocumentSession,
        targetUri: Uri,
        bookmarks: List<Bookmark>,
    ): Uri = withContext(mupdfDispatcher) {
        val pdf = session.document as? PDFDocument
            ?: throw java.io.IOException(
                "This document's bookmarks cannot be saved (not a standard PDF).",
            )
        rebuildOutline(pdf, bookmarks)

        files.takePersistableReadWrite(targetUri)
        val dir = java.io.File(appContext.cacheDir, "pdfseal_save").apply { mkdirs() }
        val temp = java.io.File.createTempFile("save_", ".pdf", dir)
        try {
            pdf.save(temp.absolutePath, "")
            files.openOutput(targetUri, "wt").use { os ->
                temp.inputStream().use { ins -> ins.copyTo(os, 64 * 1024) }
            }
        } finally {
            temp.delete()
        }
        session.bookmarksDirty = false
        targetUri
    }

    /**
     * Replace the document outline with [bookmarks] (a pre-order, depth-tagged
     * flat list). We delete every existing top-level item (each delete also
     * drops its subtree), then insert the new entries, using down()/up() to
     * recreate nesting from the depth deltas. Page targets are written as
     * `#page=N` URIs (1-based), which MuPDF resolves back to the right page.
     */
    private fun rebuildOutline(pdf: PDFDocument, bookmarks: List<Bookmark>) {
        val it = pdf.outlineIterator()
        try {
            // Drain existing outline. delete() removes the current item (and its
            // children) and lands on the next sibling; loop until empty.
            var guard = 0
            while (it.item() != null && guard++ < 100_000) {
                it.delete()
            }
            var curDepth = 0
            for (bm in bookmarks) {
                val target = bm.depth.coerceAtLeast(0)
                while (curDepth > target) {
                    it.up(); curDepth--
                }
                while (curDepth < target) {
                    // Descend into the just-inserted parent's (empty) child list.
                    // DID_NOT_MOVE means there is nothing to descend into; stop
                    // climbing and insert at the current depth instead.
                    if (it.down() ==
                        com.artifex.mupdf.fitz.OutlineIterator.ITERATOR_DID_NOT_MOVE
                    ) {
                        break
                    }
                    curDepth++
                }
                val uri = if (bm.pageIndex >= 0) "#page=${bm.pageIndex + 1}" else ""
                it.insert(bm.title, uri, true)
            }
        } finally {
            runCatching { it.destroy() }
        }
    }

    /**
     * Merge another PDF into the open document by grafting its pages in. The
     * new pages are appended to the document (so existing page indices, and
     * therefore all rotation/crop/edit state, stay valid) and placed at plan
     * position [planPos] so they appear there in the page order and export.
     * Returns the number of pages added.
     */
    suspend fun addPdf(
        session: PdfDocumentSession,
        uri: Uri,
        planPos: Int,
    ): Int = withContext(mupdfDispatcher) {
        val dst = session.document as? PDFDocument
            ?: throw java.io.IOException("Pages can't be added to this PDF.")
        files.takePersistableRead(uri)
        val temp = files.copyToCacheTempFile(uri)
        val srcDoc = try {
            Document.openDocument(temp.absolutePath)
        } catch (e: Throwable) {
            temp.delete()
            throw java.io.IOException("Could not open the PDF you picked.", e)
        }
        try {
            if (srcDoc.needsPassword()) {
                throw java.io.IOException(
                    "That PDF is password-protected, so it can't be added.",
                )
            }
            val src = srcDoc as? PDFDocument
                ?: throw java.io.IOException(
                    "The file you picked is not a standard PDF.",
                )
            val srcCount = src.countPages()
            if (srcCount <= 0) throw java.io.IOException("That PDF has no pages.")
            val oldCount = dst.countPages()
            for (k in 0 until srcCount) {
                dst.graftPage(-1, src, k) // -1 = append
            }
            val newIndices = (oldCount until dst.countPages()).toList()
            session.onPagesAppended(newIndices, planPos)
            newIndices.size
        } finally {
            runCatching { srcDoc.destroy() }
            runCatching { temp.delete() }
        }
    }

    suspend fun exportCopy(session: PdfDocumentSession, targetUri: Uri): Uri =
        withContext(mupdfDispatcher) {
            files.takePersistableReadWrite(targetUri)
            exporter.exportCopy(session, targetUri)
        }

    suspend fun closeDocument(session: PdfDocumentSession) =
        withContext(mupdfDispatcher) { session.close() }
}
