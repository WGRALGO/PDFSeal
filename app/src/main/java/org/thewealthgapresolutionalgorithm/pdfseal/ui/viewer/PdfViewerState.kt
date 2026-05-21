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
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.HighlightObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.PdfEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.StrikethroughObject
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
    /** Page-indexed OCR results from a whole-document run. Empty until then. */
    val lastOcrAll = mutableStateListOf<OcrPageResult>()
    /** Page progress during a whole-document OCR. -1 when idle. */
    var ocrProgressPage by mutableStateOf(-1)
    var ocrProgressTotal by mutableStateOf(0)
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
        android.util.Log.d(
            "PdfSeal",
            "refreshOverlay page=$pageIndex size=${overlay.size} " +
                "rects=${overlay.map { it.rectPt }}",
        )
    }

    suspend fun goTo(page: Int) {
        val s = session ?: return
        pageIndex = page.coerceIn(0, s.pageCount - 1)
        selectedId = null
        renderCurrent()
    }

    fun addTextCentered(
        text: String,
        fontSizePt: Float,
        fontFamily: String = "Sans",
        bold: Boolean = false,
        italic: Boolean = false,
    ) {
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
            fontFamily = fontFamily,
            bold = bold,
            italic = italic,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    /**
     * Auto-fit on create. We measure the typed name in the chosen TTF at the
     * same `textSize = rectHeight * 0.8` that EditObjectPainter uses for
     * export, so the box width matches what the user sees. Resizing then
     * scales the whole signature proportionally — the locked aspect ratio
     * is (textW + 2*pad) / defaultHeightPt.
     */
    fun addSignatureCentered(
        ctx: android.content.Context,
        name: String,
        style: SignatureEditObject.SignatureStyle,
        colorArgb: Int = 0xFF101010.toInt(),
    ) {
        val s = session ?: return
        val w = pageSizePt.width
        val h = pageSizePt.height
        val defaultH = 60f
        val pad = 12f
        val textW = measureSignatureWidthPt(ctx, name, style, defaultH)
        val boxW = (textW + pad * 2f).coerceIn(80f, w * 0.9f)
        val boxH = defaultH
        val obj = SignatureEditObject(
            pageIndex = pageIndex,
            rectPt = PdfRectF(
                (w - boxW) / 2f, (h - boxH) / 2f,
                (w + boxW) / 2f, (h + boxH) / 2f,
            ),
            typedName = name,
            style = style,
            colorArgb = colorArgb,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    fun measureSignatureWidthPt(
        ctx: android.content.Context,
        name: String,
        style: SignatureEditObject.SignatureStyle,
        rectHeightPt: Float,
    ): Float {
        val tf = androidx.core.content.res.ResourcesCompat
            .getFont(ctx, SignatureFonts.fontRes(style))
            ?: android.graphics.Typeface.DEFAULT
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            .apply {
                typeface = tf
                textSize = rectHeightPt * 0.8f
            }
        return p.measureText(name)
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

    /**
     * Highlight the currently selected box. Drawn ON TOP as a translucent
     * yellow band so the text underneath stays readable. (A cover-replace
     * box has an opaque white fill, so a highlight placed *under* it would
     * be hidden — that was the "highlight looks white" bug.)
     */
    fun highlightSelected() {
        val s = session ?: return
        val sel = overlay.firstOrNull { it.id == selectedId } ?: return
        val obj = HighlightObject(
            pageIndex = pageIndex,
            rectPt = sel.rectPt.normalized(),
            zOrder = 10_000,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    /** Strike a line through the centre of the selected box. On top, visual. */
    fun strikethroughSelected() {
        val s = session ?: return
        val sel = overlay.firstOrNull { it.id == selectedId } ?: return
        val obj = StrikethroughObject(
            pageIndex = pageIndex,
            rectPt = sel.rectPt.normalized(),
            zOrder = 10_001,
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
    }

    fun moveSelectedByPdf(dxPt: Float, dyPt: Float) {
        val id = selectedId ?: return
        val idx = overlay.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            ?: return
        val obj = overlay[idx]
        val r = obj.rectPt
        val nr = PdfRectF(
            r.left + dxPt, r.top + dyPt, r.right + dxPt, r.bottom + dyPt,
        )
        applyRect(idx, id, nr)
    }

    /** Resize the selected object by a PDF-point delta on its bottom-right. */
    fun resizeSelectedByPdf(dxPt: Float, dyPt: Float) {
        val id = selectedId ?: return
        val idx = overlay.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            ?: return
        val obj = overlay[idx]
        val r = obj.rectPt
        val minSide = 8f
        val nr = PdfRectF(
            r.left,
            r.top,
            (r.right + dxPt).coerceAtLeast(r.left + minSide),
            (r.bottom + dyPt).coerceAtLeast(r.top + minSide),
        )
        applyRect(idx, id, nr)
    }

    /**
     * Write [nr] into the overlay AND the session. We `.copy(rectPt = nr)`
     * the data-class subclass so Compose's SnapshotStateList sees a new
     * instance and recomposes — assigning the same reference back was the
     * "signature stuck" bug.
     */
    private fun applyRect(idx: Int, id: String, nr: PdfRectF) {
        val updated = overlay[idx].withRect(nr)
        overlay[idx] = updated
        session?.replaceEdit(id, updated)
    }

    private fun PdfEditObject.withRect(nr: PdfRectF): PdfEditObject =
        when (this) {
            is SignatureEditObject -> copy(rectPt = nr)
            is TextEditObject -> copy(rectPt = nr)
            is CoverReplaceObject -> copy(rectPt = nr)
            is HighlightObject -> copy(rectPt = nr)
            is StrikethroughObject -> copy(rectPt = nr)
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

    enum class HandleCorner { NW, NE, SW, SE }

    /** Which corner handle of the selected object is at (xPt,yPt), if any. */
    fun hitTestHandle(xPt: Float, yPt: Float, tolPt: Float): HandleCorner? {
        val id = selectedId ?: return null
        val r = overlay.firstOrNull { it.id == id }?.rectPt?.normalized()
            ?: return null
        fun near(ax: Float, ay: Float) =
            kotlin.math.abs(xPt - ax) <= tolPt &&
                kotlin.math.abs(yPt - ay) <= tolPt
        return when {
            near(r.left, r.top) -> HandleCorner.NW
            near(r.right, r.top) -> HandleCorner.NE
            near(r.left, r.bottom) -> HandleCorner.SW
            near(r.right, r.bottom) -> HandleCorner.SE
            else -> null
        }
    }

    /**
     * Aspect-locked resize. The corner *opposite* [corner] stays anchored;
     * the dragged corner follows the pointer. Width/height ratio is taken
     * from [startRect] (captured at gesture start), so the font never
     * visually stretches even across many incremental events.
     *
     * The pointer's dominant axis (width vs height, normalised by the locked
     * ratio) wins, and the other axis derives from it. That keeps movement
     * responsive instead of jittering between the two.
     */
    fun resizeSelectedByCorner(
        corner: HandleCorner,
        startRect: PdfRectF,
        pointerXPt: Float,
        pointerYPt: Float,
        minSidePt: Float = 8f,
    ) {
        val id = selectedId ?: return
        val idx = overlay.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            ?: return
        val k = startRect.width / startRect.height
        val ax = if (corner == HandleCorner.NW || corner == HandleCorner.SW) {
            startRect.right
        } else startRect.left
        val ay = if (corner == HandleCorner.NW || corner == HandleCorner.NE) {
            startRect.bottom
        } else startRect.top
        val rawW = kotlin.math.abs(pointerXPt - ax)
        val rawH = kotlin.math.abs(pointerYPt - ay)
        val newW: Float
        val newH: Float
        if (rawW / k >= rawH) {
            newW = rawW.coerceAtLeast(minSidePt); newH = newW / k
        } else {
            newH = rawH.coerceAtLeast(minSidePt / k); newW = newH * k
        }
        val nr = when (corner) {
            HandleCorner.NW -> PdfRectF(ax - newW, ay - newH, ax, ay)
            HandleCorner.NE -> PdfRectF(ax, ay - newH, ax + newW, ay)
            HandleCorner.SW -> PdfRectF(ax - newW, ay, ax, ay + newH)
            HandleCorner.SE -> PdfRectF(ax, ay, ax + newW, ay + newH)
        }
        applyRect(idx, id, nr)
    }

    /** Pinch-zoom scale applied to the selected object, around its centre. */
    fun scaleSelectedAroundCenter(factor: Float, minSidePt: Float = 8f) {
        if (factor == 1f) return
        val id = selectedId ?: return
        val idx = overlay.indexOfFirst { it.id == id }.takeIf { it >= 0 }
            ?: return
        val r = overlay[idx].rectPt.normalized()
        val cx = (r.left + r.right) / 2f
        val cy = (r.top + r.bottom) / 2f
        val newW = (r.width * factor).coerceAtLeast(minSidePt)
        val newH = (r.height * factor).coerceAtLeast(minSidePt)
        val nr = PdfRectF(
            cx - newW / 2f, cy - newH / 2f,
            cx + newW / 2f, cy + newH / 2f,
        )
        applyRect(idx, id, nr)
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
            val result = engine.ocrPage(s, pageIndex)
            lastOcr = result
            lastMessage = if (result.boxes.isEmpty()) {
                "OCR complete — no text found on this page."
            } else {
                "OCR complete · ${result.meanConfidence.toInt()}% confidence"
            }
        } catch (e: Exception) {
            lastMessage = "OCR failed on this page. Your PDF was not changed."
        } finally {
            busy = false
        }
    }

    /**
     * OCR every page. Results are appended to [lastOcrAll] in page order.
     * [lastOcr] is also set to the most recent page so the panel's per-page
     * stats stay populated. [ocrProgressPage]/[ocrProgressTotal] drive the
     * panel's progress indicator.
     */
    suspend fun runOcrDocument() {
        val s = session ?: return
        busy = true
        lastOcrAll.clear()
        ocrProgressTotal = s.pageCount
        ocrProgressPage = 0
        try {
            for (i in 0 until s.pageCount) {
                ocrProgressPage = i + 1
                val result = engine.ocrPage(s, i)
                lastOcrAll.add(result)
                lastOcr = result
            }
            lastMessage = "OCR finished on all ${s.pageCount} pages."
        } catch (e: Exception) {
            lastMessage = "OCR failed mid-document. Your PDF was not changed."
        } finally {
            busy = false
            ocrProgressPage = -1
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

    /**
     * The editable text behind the current selection. For a plain
     * [TextEditObject] that's the object itself; for a [CoverReplaceObject]
     * (used by Editable Copy and Cover & Replace) it's the first overlay
     * text, so the Edit button works on those boxes too.
     */
    fun selectedTextObject(): TextEditObject? =
        when (val o = overlay.firstOrNull { it.id == selectedId }) {
            is TextEditObject -> o
            is CoverReplaceObject -> o.overlayText.firstOrNull()
            else -> null
        }

    fun updateSelectedText(
        text: String,
        fontSizePt: Float,
        fontFamily: String = "Sans",
        bold: Boolean = false,
        italic: Boolean = false,
    ) {
        val id = selectedId ?: return
        val obj = session?.editsFor(pageIndex)?.firstOrNull { it.id == id }
            ?: return
        when (obj) {
            is TextEditObject -> {
                obj.text = text
                obj.fontSizePt = fontSizePt
                obj.fontFamily = fontFamily
                obj.bold = bold
                obj.italic = italic
            }
            is CoverReplaceObject -> {
                val inner = obj.overlayText.firstOrNull()
                if (inner != null) {
                    inner.text = text
                    inner.fontSizePt = fontSizePt
                    inner.fontFamily = fontFamily
                    inner.bold = bold
                    inner.italic = italic
                } else {
                    obj.overlayText.add(
                        TextEditObject(
                            pageIndex = obj.pageIndex,
                            rectPt = obj.rectPt,
                            text = text,
                            fontSizePt = fontSizePt,
                            fontFamily = fontFamily,
                            bold = bold,
                            italic = italic,
                        ),
                    )
                }
            }
            else -> return
        }
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
