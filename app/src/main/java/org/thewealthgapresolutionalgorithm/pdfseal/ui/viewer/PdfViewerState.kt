package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfCoordinateMapper
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.EditableCopyBuilder
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfEngine
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PanClamp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.CoverReplaceObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.PdfEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrPageResult

/**
 * UI-side holder. Talks ONLY to [PdfEngine]; never imports MuPDF. The page is
 * rendered once at [renderScale] (content-space px per PDF point); pinch zoom
 * and pan are applied by a Compose graphicsLayer over both the page image and
 * the edit overlay, so they stay aligned automatically.
 */
class PdfViewerState(private val engine: PdfEngine) {


    var session by mutableStateOf<PdfDocumentSession?>(null)
        private set
    var pageIndex by mutableStateOf(0)
        private set
    var pageBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var pageSizePt by mutableStateOf(PdfRectF(0f, 0f, 1f, 1f))
        private set
    var renderScale by mutableStateOf(2f)
        private set

    var zoom by mutableStateOf(1f)
    var panX by mutableStateOf(0f)
    var panY by mutableStateOf(0f)

    var selectedId by mutableStateOf<String?>(null)
    var coverMode by mutableStateOf(false)
    var lastOcr by mutableStateOf<OcrPageResult?>(null)
    var planVersion by mutableStateOf(0)
        private set
    var busy by mutableStateOf(false)
        private set
    var lastMessage by mutableStateOf<String?>(null)

    /** True after an open() attempt failed with no document loaded. Lets the
     *  viewer show a real error + Back instead of a forever "Loading…". */
    var openFailed by mutableStateOf(false)
        private set

    val overlay = mutableStateListOf<PdfEditObject>()

    val pageCount: Int get() = session?.pageCount ?: 0

    fun mapper() = PdfCoordinateMapper(
        pageWidthPt = pageSizePt.width,
        pageHeightPt = pageSizePt.height,
        renderScale = renderScale,
    )

    suspend fun open(uri: Uri) {
        busy = true
        openFailed = false
        try {
            session = engine.openDocument(uri)
            pageIndex = 0
            zoom = 1f; panX = 0f; panY = 0f
            renderCurrent()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Structured-cancellation must propagate, never be swallowed as
            // an "open failed". (A prior bug surfaced this as a fake error
            // and stranded the viewer on "Loading…".)
            throw e
        } catch (e: Exception) {
            // PdfEngine throws IOException with a user-facing message
            // (password/encrypted, corrupt, no pages, lost permission).
            // Surface it directly; fall back to a generic safe message.
            lastMessage = e.message?.takeIf { it.isNotBlank() }
                ?: "PDFSeal could not open this PDF. The file may be " +
                "damaged, encrypted, or unsupported."
            session = null
            openFailed = true
        } finally {
            busy = false
        }
    }

    suspend fun renderCurrent() {
        val s = session ?: return
        busy = true
        try {
            pageSizePt = s.pageSizePt(pageIndex)
            // Adaptive: keep the rendered bitmap crisp when zoomed without
            // wasting memory on huge pages. Target ~2200 px on the long edge.
            val longEdgePt = maxOf(pageSizePt.width, pageSizePt.height)
                .coerceAtLeast(1f)
            renderScale = (2200f / longEdgePt).coerceIn(2f, 4f)
            pageBitmap = engine.renderPage(s, pageIndex, renderScale)
            refreshOverlay()
        } catch (e: Exception) {
            lastMessage = "Render failed: ${e.message}"
        } finally {
            busy = false
        }
    }

    fun refreshOverlay() {
        overlay.clear()
        session?.editsFor(pageIndex)?.let { overlay.addAll(it) }
    }

    suspend fun goTo(page: Int) {
        val s = session ?: return
        pageIndex = page.coerceIn(0, s.pageCount - 1)
        selectedId = null
        renderCurrent()
    }

    fun addTextCentered(text: String, fontSizePt: Float) {
        val s = session ?: return
        val w = pageSizePt.width
        val h = pageSizePt.height
        val boxW = (w * 0.5f).coerceAtLeast(80f)
        val boxH = fontSizePt * 1.6f
        val obj = TextEditObject(
            pageIndex = pageIndex,
            rectPt = PdfRectF(
                (w - boxW) / 2f, (h - boxH) / 2f,
                (w + boxW) / 2f, (h + boxH) / 2f,
            ),
            text = text,
            fontSizePt = fontSizePt,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    fun addSignatureCentered(
        name: String,
        style: SignatureEditObject.SignatureStyle,
    ) {
        val s = session ?: return
        val w = pageSizePt.width
        val h = pageSizePt.height
        val boxW = (w * 0.4f).coerceAtLeast(120f)
        val boxH = (boxW * 0.28f).coerceAtLeast(28f)
        val obj = SignatureEditObject(
            pageIndex = pageIndex,
            rectPt = PdfRectF(
                (w - boxW) / 2f, (h - boxH) / 2f,
                (w + boxW) / 2f, (h + boxH) / 2f,
            ),
            typedName = name,
            style = style,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    /** Cover & Replace — visual cover only, NOT secure redaction. */
    fun addCover(rectPt: PdfRectF) {
        val s = session ?: return
        val n = rectPt.normalized()
        if (n.width < 2f || n.height < 2f) return
        val obj = CoverReplaceObject(
            pageIndex = pageIndex,
            rectPt = n,
            zOrder = -1, // under any replacement text added on top
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
        coverMode = false
    }

    fun moveSelectedByPdf(dxPt: Float, dyPt: Float) {
        val id = selectedId ?: return
        val obj = overlay.firstOrNull { it.id == id } ?: return
        val r = obj.rectPt
        obj.rectPt = PdfRectF(
            r.left + dxPt, r.top + dyPt, r.right + dxPt, r.bottom + dyPt,
        )
        session?.hasUnsavedEdits = true
        // Trigger recomposition by replacing the list element.
        val idx = overlay.indexOfFirst { it.id == id }
        if (idx >= 0) overlay[idx] = obj
    }

    /** Resize the selected object by a PDF-point delta on its bottom-right. */
    fun resizeSelectedByPdf(dxPt: Float, dyPt: Float) {
        val id = selectedId ?: return
        val obj = overlay.firstOrNull { it.id == id } ?: return
        val r = obj.rectPt
        val minSide = 8f
        obj.rectPt = PdfRectF(
            r.left,
            r.top,
            (r.right + dxPt).coerceAtLeast(r.left + minSide),
            (r.bottom + dyPt).coerceAtLeast(r.top + minSide),
        )
        session?.hasUnsavedEdits = true
        val idx = overlay.indexOfFirst { it.id == id }
        if (idx >= 0) overlay[idx] = obj
    }

    /** Topmost object id whose point-space rect contains (xPt,yPt), or null. */
    fun hitTest(xPt: Float, yPt: Float): String? =
        overlay
            .sortedByDescending { it.zOrder }
            .firstOrNull { o ->
                val r = o.rectPt.normalized()
                xPt >= r.left && xPt <= r.right &&
                    yPt >= r.top && yPt <= r.bottom
            }
            ?.id

    /** True if (xPt,yPt) is on the selected object's bottom-right handle. */
    fun isOnResizeHandle(xPt: Float, yPt: Float, tolPt: Float): Boolean {
        val id = selectedId ?: return false
        val r = overlay.firstOrNull { it.id == id }?.rectPt ?: return false
        return kotlin.math.abs(xPt - r.right) <= tolPt &&
            kotlin.math.abs(yPt - r.bottom) <= tolPt
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        val obj = session?.editsFor(pageIndex)?.firstOrNull { it.id == id } ?: return
        session?.removeEdit(obj)
        selectedId = null
        refreshOverlay()
    }

    suspend fun runOcrCurrent() {
        val s = session ?: return
        busy = true
        try {
            lastOcr = engine.ocrPage(s, pageIndex)
        } catch (e: Exception) {
            lastMessage = "OCR failed on this page. Your PDF was not changed."
        } finally {
            busy = false
        }
    }

    /**
     * OCR the page and turn recognised lines into editable text overlays.
     * This is OCR-based reconstruction — review/correct before export.
     */
    suspend fun makeEditableCopyCurrent() {
        val s = session ?: return
        busy = true
        try {
            val ocr = engine.ocrPage(s, pageIndex)
            lastOcr = ocr
            val overlays = EditableCopyBuilder.buildOverlays(ocr)
            overlays.forEach { s.addEdit(it) }
            refreshOverlay()
            lastMessage = if (overlays.isEmpty()) {
                "No text recognised on this page."
            } else {
                "${overlays.size} editable text boxes created. " +
                    org.thewealthgapresolutionalgorithm.pdfseal.ui
                        .HonestCopy.OCR_REVIEW_WARNING
            }
        } catch (e: Exception) {
            lastMessage = "OCR failed on this page. Your PDF was not changed."
        } finally {
            busy = false
        }
    }

    fun selectedObject(): PdfEditObject? =
        overlay.firstOrNull { it.id == selectedId }

    /**
     * Clamp pan so the page can never be flung fully off-screen — at least
     * 20% of the smaller of (page, viewport) stays visible on each axis.
     * Call after changing zoom/pan from the gesture handler, passing the
     * unscaled content size and the viewport size in px.
     */
    fun clampPan(contentWpx: Float, contentHpx: Float, vpW: Float, vpH: Float) {
        panX = PanClamp.clampAxis(panX, contentWpx * zoom, vpW)
        panY = PanClamp.clampAxis(panY, contentHpx * zoom, vpH)
    }

    fun selectedTextObject(): TextEditObject? =
        overlay.firstOrNull { it.id == selectedId } as? TextEditObject

    fun updateSelectedText(text: String, fontSizePt: Float) {
        val id = selectedId ?: return
        val obj = session?.editsFor(pageIndex)
            ?.firstOrNull { it.id == id } as? TextEditObject ?: return
        obj.text = text
        obj.fontSizePt = fontSizePt
        session?.hasUnsavedEdits = true
        refreshOverlay()
    }

    suspend fun thumbnail(pageIndex: Int): android.graphics.Bitmap? {
        val s = session ?: return null
        return try {
            engine.renderThumbnail(s, pageIndex, maxEdgePx = 220)
        } catch (e: Exception) {
            null
        }
    }

    // ---- Page tools (export plan; viewer navigation stays on source pages) ----

    fun exportPlan(): List<Pair<Int, Int>> {
        planVersion // read for recomposition
        val s = session ?: return emptyList()
        return s.exportOrder.map { src -> src to (s.extraRotation[src] ?: 0) }
    }

    fun rotatePagePlan(srcIndex: Int) {
        session?.rotatePage(srcIndex, 90); planVersion++
    }

    fun deletePagePlan(srcIndex: Int) {
        session?.deletePageFromExport(srcIndex); planVersion++
    }

    fun movePagePlan(fromPos: Int, toPos: Int) {
        session?.movePage(fromPos, toPos); planVersion++
    }

    fun resetPlan() {
        session?.resetExportPlan(); planVersion++
    }

    /** True if any page has a Cover & Replace object (not secure redaction). */
    fun usesCoverReplace(): Boolean =
        session?.edits?.values?.any { list ->
            list.any { it is org.thewealthgapresolutionalgorithm.pdfseal
                .engine.edit.CoverReplaceObject }
        } ?: false

    /**
     * Default export filename: `original-name-PDFSeal-copy.pdf`. The export
     * goes through the Storage Access Framework create-document flow, so the
     * user picks the destination and Android creates a NEW document there —
     * the source URI is never written. If the chosen folder already has this
     * name, Android's document provider keeps both (it does not overwrite).
     */
    fun defaultExportName(): String {
        val raw = session?.displayName ?: "document.pdf"
        val base = raw.substringBeforeLast('.', raw).ifBlank { "document" }
        return "$base-PDFSeal-copy.pdf"
    }

    suspend fun export(targetUri: Uri) {
        val s = session ?: return
        busy = true
        try {
            engine.exportCopy(s, targetUri)
            lastMessage = "Exported a flattened copy. Original unchanged."
        } catch (e: Exception) {
            lastMessage =
                "Export failed. Your original PDF was not changed."
        } finally {
            busy = false
        }
    }
}
