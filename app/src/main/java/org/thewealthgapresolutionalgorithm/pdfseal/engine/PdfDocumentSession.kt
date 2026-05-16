package org.thewealthgapresolutionalgorithm.pdfseal.engine

import android.net.Uri
import com.artifex.mupdf.fitz.Document
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.PdfEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrPageResult

/**
 * One open PDF. Wraps the MuPDF [Document]. NOT thread-safe — all access must
 * be confined to the engine's single MuPDF dispatcher (see [PdfEngine]).
 *
 * Holds in-memory, unsaved editing state (edit objects + OCR cache). Nothing
 * here mutates the source file; export produces a separate copy.
 */
class PdfDocumentSession internal constructor(
    val sourceUri: Uri,
    val displayName: String,
    internal val document: Document,
) {
    val pageCount: Int = document.countPages()

    private val pageSizeCache = HashMap<Int, PdfRectF>()

    /** Unsaved objects the user has placed, keyed by page index. */
    val edits: MutableMap<Int, MutableList<PdfEditObject>> = HashMap()

    /** OCR results cached per page so we never re-OCR unnecessarily. */
    val ocrResults: MutableMap<Int, OcrPageResult> = HashMap()

    var hasUnsavedEdits: Boolean = false
        internal set

    /** Page size in PDF points (after the page's own /Rotate). */
    fun pageSizePt(pageIndex: Int): PdfRectF = pageSizeCache.getOrPut(pageIndex) {
        val page = document.loadPage(pageIndex)
        try {
            val b = page.bounds
            PdfRectF(b.x0, b.y0, b.x1, b.y1).normalized()
        } finally {
            page.destroy()
        }
    }

    fun editsFor(pageIndex: Int): MutableList<PdfEditObject> =
        edits.getOrPut(pageIndex) { mutableListOf() }

    fun addEdit(obj: PdfEditObject) {
        editsFor(obj.pageIndex).add(obj)
        hasUnsavedEdits = true
    }

    fun removeEdit(obj: PdfEditObject) {
        edits[obj.pageIndex]?.remove(obj)
        hasUnsavedEdits = true
    }

    internal fun close() {
        runCatching { document.destroy() }
    }
}
