package org.thewealthgapresolutionalgorithm.pdfseal.engine

import android.net.Uri
import com.artifex.mupdf.fitz.Document
import java.io.File
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
    /** Private cache copy MuPDF opened by path; deleted on [close]. */
    private val tempFile: File? = null,
) {
    // Dynamic: grows when another PDF is merged in via graft.
    val pageCount: Int get() = document.countPages()

    private val pageSizeCache = HashMap<Int, PdfRectF>()

    /** Unsaved objects the user has placed, keyed by page index. */
    val edits: MutableMap<Int, MutableList<PdfEditObject>> = HashMap()

    /** OCR results cached per page so we never re-OCR unnecessarily. */
    val ocrResults: MutableMap<Int, OcrPageResult> = HashMap()

    /**
     * Working copy of the document outline (bookmarks). Populated once from the
     * PDF by [PdfEngine.loadBookmarks]; the user's add/delete edit this list.
     * It is written back to a real PDF only by a "Save with bookmarks" save,
     * never by the raster flatten Export.
     */
    val bookmarks: MutableList<Bookmark> = mutableListOf()
    var bookmarksLoaded: Boolean = false
        internal set
    var bookmarksDirty: Boolean = false
        internal set

    var hasUnsavedEdits: Boolean = false
        internal set

    /**
     * Export plan — applied only at export time (the viewer still navigates the
     * original pages). [exportOrder] is the ordered list of source page
     * indices to write; deleting a page removes it here. [extraRotation] maps a
     * source page index to an additional clockwise rotation in degrees
     * (0/90/180/270) layered on top of the page's own /Rotate.
     */
    val exportOrder: MutableList<Int> = (0 until pageCount).toMutableList()
    val extraRotation: MutableMap<Int, Int> = HashMap()

    /**
     * Per-source-page crop, stored as fractional insets (0..1) of the ROTATED
     * page: left/top/right/bottom. Null/absent = no crop (full page). Fractions
     * compose cleanly with rotation and let one "crop all pages" rect apply to
     * pages of different sizes.
     */
    val cropFractions: MutableMap<Int, PdfRectF> = HashMap()

    fun cropOf(srcIndex: Int): PdfRectF? = cropFractions[srcIndex]

    fun setCrop(srcIndex: Int, frac: PdfRectF) {
        cropFractions[srcIndex] = frac
        hasUnsavedEdits = true
    }

    fun setCropAllPages(frac: PdfRectF) {
        for (i in 0 until pageCount) cropFractions[i] = frac
        hasUnsavedEdits = true
    }

    fun clearCrop(srcIndex: Int) {
        cropFractions.remove(srcIndex)
        hasUnsavedEdits = true
    }

    fun rotatePage(srcIndex: Int, deltaDeg: Int) {
        val cur = extraRotation[srcIndex] ?: 0
        extraRotation[srcIndex] = ((cur + deltaDeg) % 360 + 360) % 360
        hasUnsavedEdits = true
    }

    fun deletePageFromExport(srcIndex: Int) {
        if (exportOrder.size > 1) {
            exportOrder.remove(srcIndex)
            hasUnsavedEdits = true
        }
    }

    fun movePage(fromPos: Int, toPos: Int) {
        if (fromPos in exportOrder.indices && toPos in exportOrder.indices) {
            val v = exportOrder.removeAt(fromPos)
            exportOrder.add(toPos, v)
            hasUnsavedEdits = true
        }
    }

    /**
     * Record that [newIndices] (freshly grafted, physically appended source
     * pages) should appear in the plan starting at position [planPos]. Existing
     * source indices are unchanged, so rotation/crop/edit maps stay valid.
     */
    fun onPagesAppended(newIndices: List<Int>, planPos: Int) {
        val pos = planPos.coerceIn(0, exportOrder.size)
        exportOrder.addAll(pos, newIndices)
        hasUnsavedEdits = true
    }

    fun resetExportPlan() {
        exportOrder.clear()
        exportOrder.addAll(0 until pageCount)
        extraRotation.clear()
        cropFractions.clear()
        hasUnsavedEdits = true
    }

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

    /** Current extra rotation (0/90/180/270) applied to a source page. */
    fun rotationOf(srcIndex: Int): Int =
        ((extraRotation[srcIndex] ?: 0) % 360 + 360) % 360

    /**
     * Page size in PDF points AFTER the current extra rotation — width/height
     * are swapped for 90°/270°. This is the coordinate space the viewer and the
     * exporter both place edit overlays in, so everything stays aligned.
     */
    fun rotatedPageSizePt(srcIndex: Int): PdfRectF {
        val b = pageSizePt(srcIndex)
        return if (rotationOf(srcIndex).let { it == 90 || it == 270 }) {
            PdfRectF(0f, 0f, b.height, b.width)
        } else {
            PdfRectF(0f, 0f, b.width, b.height)
        }
    }

    /**
     * Page size in PDF points as actually DISPLAYED/exported: rotated, then
     * shrunk by any crop. This is the coordinate space the viewer, overlays,
     * gestures, and exporter all use, so everything stays aligned.
     */
    fun displayPageSizePt(srcIndex: Int): PdfRectF {
        val r = rotatedPageSizePt(srcIndex)
        val c = cropOf(srcIndex) ?: return r
        return PdfRectF(
            0f, 0f,
            (r.width * (c.right - c.left)).coerceAtLeast(1f),
            (r.height * (c.bottom - c.top)).coerceAtLeast(1f),
        )
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

    /**
     * Replace the edit with [id] by [replacement] (same id, new geometry/state).
     * Used after move/resize so the session matches the viewer's overlay copy.
     */
    fun replaceEdit(id: String, replacement: PdfEditObject) {
        val list = edits[replacement.pageIndex] ?: return
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = replacement
            hasUnsavedEdits = true
        }
    }

    internal fun close() {
        runCatching { document.destroy() }
        runCatching { tempFile?.delete() }
    }
}
