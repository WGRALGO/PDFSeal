package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.thewealthgapresolutionalgorithm.pdfseal.engine.Bookmark
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfCoordinateMapper
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfEngine
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PanClamp
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.CoverReplaceObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.HighlightObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.PdfEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.SignatureEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.StrikethroughObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrBox
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

    /**
     * Position in the page PLAN (export order) the viewer is showing. The
     * viewer navigates the plan, so delete/add/reorder are WYSIWYG. The actual
     * source page to render/edit is [pageIndex], derived from this.
     */
    var planPos by mutableStateOf(0)
        private set

    /** Source page index for the current [planPos] — used for render & edits. */
    val pageIndex: Int
        get() = session?.exportOrder?.getOrNull(planPos)
            ?: planPos.coerceAtLeast(0)

    /** Number of pages the viewer pages through (plan size, not raw doc count). */
    val navCount: Int get() = session?.exportOrder?.size ?: 0
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
    /** Drag-to-crop mode. While on, the page accepts a crop rectangle drag. */
    var cropMode by mutableStateOf(false)
    /**
     * A crop rectangle the user just dragged, expressed as fractions (0..1) of
     * the FULL rotated page, waiting for the this-page / all-pages choice. The
     * viewer shows the choice dialog while this is non-null.
     */
    var pendingCropFrac by mutableStateOf<PdfRectF?>(null)
    /**
     * Tap-to-edit mode. When on, the page shows NO editable boxes (it stays
     * clean and readable); tapping a line of text turns just that one line into
     * an editable overlay. Entered from the "Edit" button after OCR runs.
     */
    var editTapMode by mutableStateOf(false)
    /** Set by [tapToEdit]; the viewer opens the text editor when it flips true. */
    var openEditDialogRequested by mutableStateOf(false)
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

    /** Document outline shown in the Bookmarks dialog. Loaded once on open. */
    val bookmarks = mutableStateListOf<Bookmark>()

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
            planPos = 0
            zoom = 1f; panX = 0f; panY = 0f
            renderCurrent()
            loadBookmarks()
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
            // Display size: rotated then cropped. The viewer works in the same
            // coordinate space the exporter writes, so overlays placed here
            // line up on export.
            pageSizePt = s.displayPageSizePt(pageIndex)
            // Adaptive: keep the rendered bitmap crisp when zoomed without
            // wasting memory on huge pages. Target ~2200 px on the long edge.
            val longEdgePt = maxOf(pageSizePt.width, pageSizePt.height)
                .coerceAtLeast(1f)
            renderScale = (2200f / longEdgePt).coerceIn(2f, 4f)
            pageBitmap = engine.renderPage(
                s, pageIndex, renderScale, s.rotationOf(pageIndex),
                s.cropOf(pageIndex),
            )
            refreshOverlay()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PdfSeal", "renderCurrent failed page=$pageIndex", e)
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

    /** Navigate to a position in the PLAN (0-based). */
    suspend fun goToPlan(position: Int) {
        val s = session ?: return
        val last = (s.exportOrder.size - 1).coerceAtLeast(0)
        planPos = position.coerceIn(0, last)
        selectedId = null
        renderCurrent()
    }

    /** Navigate to whatever plan position holds source page [srcIndex]. */
    suspend fun goToSource(srcIndex: Int) {
        val s = session ?: return
        val pos = s.exportOrder.indexOf(srcIndex)
        if (pos >= 0) goToPlan(pos) else goToPlan(planPos)
    }

    /**
     * Navigate within the plan, and if we're in tap-to-edit mode also (re-)OCR
     * the landing page so tapping a line works there. Used by Prev/Next; plain
     * navigation (e.g. the Pages thumbnails) uses [goToPlan].
     */
    suspend fun goToForEditing(position: Int) {
        goToPlan(position)
        if (editTapMode) ensureOcrCurrent()
    }

    // ---- Bookmarks (document outline) ----

    suspend fun loadBookmarks() {
        val list = engine.loadBookmarks(session ?: return)
        bookmarks.clear()
        bookmarks.addAll(list)
    }

    /** Add a top-level bookmark pointing at the page currently being viewed. */
    fun addBookmark(title: String) {
        val s = session ?: return
        val name = title.trim().ifBlank { "Page ${pageIndex + 1}" }
        val bm = Bookmark(title = name, pageIndex = pageIndex, depth = 0)
        bookmarks.add(bm)
        s.bookmarks.add(bm)
        s.bookmarksDirty = true
    }

    fun deleteBookmark(id: String) {
        val s = session ?: return
        bookmarks.removeAll { it.id == id }
        s.bookmarks.removeAll { it.id == id }
        s.bookmarksDirty = true
    }

    val bookmarksDirty: Boolean get() = session?.bookmarksDirty ?: false

    fun defaultBookmarkSaveName(): String {
        val raw = session?.displayName ?: "document.pdf"
        val base = raw.substringBeforeLast('.', raw).ifBlank { "document" }
        return "$base-bookmarks.pdf"
    }

    suspend fun saveWithBookmarks(targetUri: Uri) {
        val s = session ?: return
        busy = true
        try {
            engine.saveWithBookmarks(s, targetUri, bookmarks.toList())
            lastMessage = "Saved a PDF with your bookmarks. Original unchanged."
        } catch (e: Exception) {
            lastMessage =
                "Could not save bookmarks. Your original PDF was not changed."
        } finally {
            busy = false
        }
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
        // Fit the box to the text so the handles aren't oversized and resizing
        // scales the text (font derives from box height in EditObjectPainter).
        val pad = 6f
        val boxH = fontSizePt * 1.25f // rendered font ≈ boxH*0.8 = fontSizePt
        val textW = measureTextWidthPt(text, fontSizePt, fontFamily, bold, italic)
        val boxW = (textW + pad * 2f).coerceIn(40f, w * 0.95f)
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

    private fun measureTextWidthPt(
        text: String,
        sizePt: Float,
        family: String,
        bold: Boolean,
        italic: Boolean,
    ): Float {
        val base = when (family) {
            "Serif" -> android.graphics.Typeface.SERIF
            "Mono" -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.SANS_SERIF
        }
        val style = when {
            bold && italic -> android.graphics.Typeface.BOLD_ITALIC
            bold -> android.graphics.Typeface.BOLD
            italic -> android.graphics.Typeface.ITALIC
            else -> android.graphics.Typeface.NORMAL
        }
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            .apply {
                textSize = sizePt
                typeface = android.graphics.Typeface.create(base, style)
            }
        return p.measureText(text.ifEmpty { "Text" })
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
     * Enter tap-to-edit mode. We OCR the current page so we know where the text
     * lines are, but we do NOT create any overlay boxes — the page stays clean.
     * The user then taps a line and only that line becomes editable
     * ([tapToEdit]). This replaces the old "flood the page with boxes" Edit.
     */
    suspend fun enterEditMode() {
        val s = session ?: return
        busy = true
        try {
            val ocr = engine.ocrPage(s, pageIndex)
            lastOcr = ocr
            editTapMode = true
            selectedId = null
            lastMessage = if (ocr.boxes.isEmpty()) {
                "No editable text recognised on this page."
            } else {
                "Tap a line of text to edit it. " +
                    org.thewealthgapresolutionalgorithm.pdfseal.ui
                        .HonestCopy.OCR_REVIEW_WARNING
            }
        } catch (e: Exception) {
            lastMessage = "OCR failed on this page. Your PDF was not changed."
        } finally {
            busy = false
        }
    }

    fun exitEditMode() {
        editTapMode = false
    }

    /** Make sure OCR exists for the current page (used when paging in edit mode). */
    suspend fun ensureOcrCurrent() {
        val s = session ?: return
        if (s.ocrResults[pageIndex] == null) {
            busy = true
            try {
                lastOcr = engine.ocrPage(s, pageIndex)
            } catch (e: Exception) {
                lastMessage = "OCR failed on this page."
            } finally {
                busy = false
            }
        } else {
            lastOcr = s.ocrResults[pageIndex]
        }
    }

    /**
     * Tap-to-edit: find the OCR text line under (xPt,yPt) on the current page,
     * turn just that one line into an editable Cover & Replace overlay, select
     * it, and ask the viewer to open the text editor. Returns true if a line
     * was hit. The rest of the page is left untouched.
     */
    fun tapToEdit(xPt: Float, yPt: Float): Boolean {
        val s = session ?: return false
        val ocr = s.ocrResults[pageIndex] ?: lastOcr?.takeIf { it.pageIndex == pageIndex }
            ?: return false
        if (ocr.renderedBitmapWidthPx == 0 || ocr.renderedBitmapHeightPx == 0) {
            return false
        }
        val sx = ocr.pdfPageWidthPt / ocr.renderedBitmapWidthPx
        val sy = ocr.pdfPageHeightPt / ocr.renderedBitmapHeightPx
        val hit = ocr.boxes
            .asSequence()
            .filter { it.level == OcrBox.Level.LINE }
            .map { box ->
                val r = box.boundsBitmapPx
                box to PdfRectF(
                    r.left * sx, r.top * sy, r.right * sx, r.bottom * sy,
                ).normalized()
            }
            .filter { (_, r) ->
                xPt >= r.left && xPt <= r.right && yPt >= r.top && yPt <= r.bottom
            }
            .minByOrNull { (_, r) -> r.width * r.height }
            ?: return false

        val coverRect = hit.second.padded(2f)
        val fontPt = (coverRect.height * 0.72f).coerceIn(6f, 96f)
        val text = TextEditObject(
            pageIndex = pageIndex,
            rectPt = coverRect,
            text = hit.first.text.trim(),
            fontSizePt = fontPt,
        )
        val obj = CoverReplaceObject(
            pageIndex = pageIndex,
            rectPt = coverRect,
            fillArgb = 0xFFFFFFFF.toInt(),
            overlayText = mutableListOf(text),
        )
        s.addEdit(obj)
        refreshOverlay()
        selectedId = obj.id
        openEditDialogRequested = true
        return true
    }

    private fun PdfRectF.padded(pt: Float): PdfRectF =
        PdfRectF(left - pt, top - pt, right + pt, bottom + pt)

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
                // Refit the box (font derives from box height now), keeping the
                // top-left corner where the user placed it.
                val r = obj.rectPt
                val pad = 6f
                val boxH = fontSizePt * 1.25f
                val textW = measureTextWidthPt(text, fontSizePt, fontFamily, bold, italic)
                val boxW = (textW + pad * 2f).coerceIn(40f, pageSizePt.width * 0.95f)
                session?.replaceEdit(
                    id,
                    obj.copy(
                        rectPt = PdfRectF(r.left, r.top, r.left + boxW, r.top + boxH),
                        text = text,
                        fontSizePt = fontSizePt,
                        fontFamily = fontFamily,
                        bold = bold,
                        italic = italic,
                    ),
                )
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
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

    /**
     * Rotate the page currently being viewed by [deltaDeg] (use +90 for the
     * right arrow, -90 for the left) and re-render so it turns live. Rotation
     * is MuPDF-level, so any overlays already on the page move with it.
     */
    suspend fun rotateCurrentPage(deltaDeg: Int) {
        val s = session ?: return
        s.rotatePage(pageIndex, deltaDeg)
        planVersion++
        renderCurrent()
    }

    /**
     * Delete pages by a 1-based spec like "3", "3-7", or "2,5,9-11". Numbers
     * refer to positions in the current page plan. At least one page is always
     * kept. Returns how many were removed.
     */
    fun deletePagesByRange(spec: String): Int {
        val s = session ?: return 0
        val total = s.exportOrder.size
        val positions = parsePageRange(spec, total) // 0-based, sorted desc
        var deleted = 0
        for (pos in positions) {
            if (s.exportOrder.size <= 1) break
            if (pos in s.exportOrder.indices) {
                s.exportOrder.removeAt(pos)
                deleted++
            }
        }
        if (deleted > 0) {
            s.hasUnsavedEdits = true
            planVersion++
            // Keep the viewer position valid after pages leave the plan.
            planPos = planPos.coerceIn(0, (s.exportOrder.size - 1).coerceAtLeast(0))
        }
        return deleted
    }

    /** Parse "3", "3-7", "2,5,9-11" into a descending list of 0-based indices. */
    private fun parsePageRange(spec: String, total: Int): List<Int> {
        val out = sortedSetOf<Int>()
        spec.split(',').forEach { token ->
            val t = token.trim()
            if (t.isEmpty()) return@forEach
            val dash = t.indexOf('-')
            if (dash > 0) {
                val a = t.substring(0, dash).trim().toIntOrNull()
                val b = t.substring(dash + 1).trim().toIntOrNull()
                if (a != null && b != null) {
                    for (n in minOf(a, b)..maxOf(a, b)) {
                        if (n in 1..total) out.add(n - 1)
                    }
                }
            } else {
                t.toIntOrNull()?.let { if (it in 1..total) out.add(it - 1) }
            }
        }
        return out.sortedDescending()
    }

    // ---- Crop ----

    /**
     * Turn a drag rectangle (in current DISPLAY points — the rotated, possibly
     * already-cropped page the user sees) into crop fractions of the FULL
     * rotated page, composing with any existing crop, and stash it in
     * [pendingCropFrac] for the this-page / all-pages choice.
     */
    fun beginCropFromDrag(rectDisplayPt: PdfRectF) {
        val s = session ?: return
        val dispW = pageSizePt.width.coerceAtLeast(1f)
        val dispH = pageSizePt.height.coerceAtLeast(1f)
        val r = rectDisplayPt.normalized()
        val cur = s.cropOf(pageIndex) ?: PdfRectF(0f, 0f, 1f, 1f)
        val cw = cur.right - cur.left
        val ch = cur.bottom - cur.top
        var nl = cur.left + (r.left / dispW) * cw
        var nr = cur.left + (r.right / dispW) * cw
        var nt = cur.top + (r.top / dispH) * ch
        var nb = cur.top + (r.bottom / dispH) * ch
        nl = nl.coerceIn(0f, 1f); nr = nr.coerceIn(0f, 1f)
        nt = nt.coerceIn(0f, 1f); nb = nb.coerceIn(0f, 1f)
        // Ignore a too-small crop (e.g. an accidental tap).
        if (nr - nl < 0.05f || nb - nt < 0.05f) {
            cropMode = false
            lastMessage = "Crop area too small — try dragging a larger box."
            return
        }
        pendingCropFrac = PdfRectF(nl, nt, nr, nb)
        cropMode = false
    }

    suspend fun applyPendingCrop(allPages: Boolean) {
        val s = session ?: return
        val frac = pendingCropFrac ?: return
        if (allPages) s.setCropAllPages(frac) else s.setCrop(pageIndex, frac)
        pendingCropFrac = null
        planVersion++
        renderCurrent()
        lastMessage = if (allPages) "Cropped all pages." else "Cropped this page."
    }

    fun cancelPendingCrop() {
        pendingCropFrac = null
    }

    /** Remove the crop on the current page (back to full page). */
    suspend fun clearCropCurrent() {
        val s = session ?: return
        s.clearCrop(pageIndex)
        planVersion++
        renderCurrent()
        lastMessage = "Crop cleared on this page."
    }

    val currentPageCropped: Boolean get() = session?.cropOf(pageIndex) != null

    // ---- Add another PDF (merge) ----

    /** A picked PDF awaiting the insert-position choice (Start/After/End). */
    var pendingAddPdfUri by mutableStateOf<Uri?>(null)

    val planSize: Int get() = session?.exportOrder?.size ?: 0

    /** Plan position just after the page currently viewed (for "after current"). */
    fun planPositionAfterCurrent(): Int {
        val s = session ?: return 0
        val idx = s.exportOrder.indexOf(pageIndex)
        return if (idx >= 0) idx + 1 else s.exportOrder.size
    }

    suspend fun addPdf(uri: Uri, insertPos: Int) {
        val s = session ?: return
        busy = true
        try {
            val n = engine.addPdf(s, uri, insertPos)
            planVersion++
            lastMessage = "Added $n page(s)."
            // Show the first added page at its plan position.
            goToPlan(insertPos.coerceIn(0, s.exportOrder.size - 1))
        } catch (e: Exception) {
            lastMessage = e.message ?: "Could not add that PDF."
        } finally {
            busy = false
        }
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            android.util.Log.e("PdfSeal", "export failed", e)
            lastMessage =
                "Export failed (${e.javaClass.simpleName}). Original unchanged."
        } finally {
            busy = false
        }
    }
}
