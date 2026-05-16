package org.thewealthgapresolutionalgorithm.pdfseal.ui.viewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfCoordinateMapper
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfEngine
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.PdfEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject

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
    var busy by mutableStateOf(false)
        private set
    var lastMessage by mutableStateOf<String?>(null)

    val overlay = mutableStateListOf<PdfEditObject>()

    val pageCount: Int get() = session?.pageCount ?: 0

    fun mapper() = PdfCoordinateMapper(
        pageWidthPt = pageSizePt.width,
        pageHeightPt = pageSizePt.height,
        renderScale = renderScale,
    )

    suspend fun open(uri: Uri) {
        busy = true
        try {
            session = engine.openDocument(uri)
            pageIndex = 0
            zoom = 1f; panX = 0f; panY = 0f
            renderCurrent()
        } catch (e: Exception) {
            lastMessage = "Open failed: ${e.message}"
        } finally {
            busy = false
        }
    }

    suspend fun renderCurrent() {
        val s = session ?: return
        busy = true
        try {
            pageSizePt = s.pageSizePt(pageIndex)
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

    fun deleteSelected() {
        val id = selectedId ?: return
        val obj = session?.editsFor(pageIndex)?.firstOrNull { it.id == id } ?: return
        session?.removeEdit(obj)
        selectedId = null
        refreshOverlay()
    }

    suspend fun export(targetUri: Uri) {
        val s = session ?: return
        busy = true
        try {
            engine.exportCopy(s, targetUri)
            lastMessage = "Exported. Original unchanged."
        } catch (e: Exception) {
            lastMessage = "Export failed: ${e.message}"
        } finally {
            busy = false
        }
    }
}
