package org.thewealthgapresolutionalgorithm.pdfseal.engine

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.android.AndroidDrawDevice

/**
 * Renders pages to Android bitmaps at a requested scale (pixels per PDF point).
 *
 * Must be called only on the engine's MuPDF dispatcher.
 */
class PdfPageRenderer {

    /** DPI / 72 gives pixels-per-point. 72 = 1.0 scale (one px per point). */
    fun scaleForDpi(dpi: Int): Float = dpi / 72f

    /**
     * @param renderScale pixels per PDF point (e.g. 2f ≈ 144 dpi).
     */
    fun renderPage(
        session: PdfDocumentSession,
        pageIndex: Int,
        renderScale: Float,
    ): Bitmap {
        require(renderScale > 0f) { "renderScale must be > 0" }
        val page = session.document.loadPage(pageIndex)
        try {
            val ctm = Matrix(renderScale, renderScale)
            val bbox = RectI(page.bounds.transform(ctm))
            val w = (bbox.x1 - bbox.x0).coerceAtLeast(1)
            val h = (bbox.y1 - bbox.y0).coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            val dev = AndroidDrawDevice(bitmap, bbox.x0, bbox.y0)
            try {
                page.run(dev, ctm, Cookie())
            } finally {
                dev.close()
                dev.destroy()
            }
            return bitmap
        } finally {
            page.destroy()
        }
    }

    /** Thumbnail at a target longest-edge pixel size. */
    fun renderThumbnail(
        session: PdfDocumentSession,
        pageIndex: Int,
        maxEdgePx: Int = 256,
    ): Bitmap {
        val size = session.pageSizePt(pageIndex)
        val longest = maxOf(size.width, size.height).coerceAtLeast(1f)
        val scale = maxEdgePx / longest
        return renderPage(session, pageIndex, scale)
    }
}
