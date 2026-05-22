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
     * @param extraRotationDeg additional clockwise rotation (0/90/180/270)
     *   applied on top of the page's own /Rotate. Baked into the render matrix
     *   (MuPDF-level), so the returned bitmap is already rotated and its
     *   width/height reflect the rotation. The bbox math below auto-sizes and
     *   re-origins the rotated page, so callers just get an upright bitmap.
     */
    fun renderPage(
        session: PdfDocumentSession,
        pageIndex: Int,
        renderScale: Float,
        extraRotationDeg: Int = 0,
        cropFrac: PdfRectF? = null,
    ): Bitmap {
        require(renderScale > 0f) { "renderScale must be > 0" }
        val page = session.document.loadPage(pageIndex)
        try {
            val ctm = Matrix(renderScale, renderScale)
            val rot = ((extraRotationDeg % 360) + 360) % 360
            if (rot != 0) ctm.rotate(rot.toFloat())
            val full = RectI(page.bounds.transform(ctm))
            // Crop is a fractional inset of the rotated page; restrict the draw
            // device to that sub-rectangle so MuPDF only renders the crop.
            val bbox = if (cropFrac != null) {
                val fw = full.x1 - full.x0
                val fh = full.y1 - full.y0
                RectI(
                    full.x0 + Math.round(cropFrac.left * fw),
                    full.y0 + Math.round(cropFrac.top * fh),
                    full.x0 + Math.round(cropFrac.right * fw),
                    full.y0 + Math.round(cropFrac.bottom * fh),
                )
            } else {
                full
            }
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

    /** Thumbnail at a target longest-edge pixel size, honouring extra rotation. */
    fun renderThumbnail(
        session: PdfDocumentSession,
        pageIndex: Int,
        maxEdgePx: Int = 256,
    ): Bitmap {
        val size = session.displayPageSizePt(pageIndex)
        val longest = maxOf(size.width, size.height).coerceAtLeast(1f)
        val scale = maxEdgePx / longest
        return renderPage(
            session, pageIndex, scale,
            session.rotationOf(pageIndex), session.cropOf(pageIndex),
        )
    }
}
