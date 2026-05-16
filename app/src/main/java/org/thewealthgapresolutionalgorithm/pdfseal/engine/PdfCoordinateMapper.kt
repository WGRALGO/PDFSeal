package org.thewealthgapresolutionalgorithm.pdfseal.engine

/**
 * Pure coordinate math. NO Android or MuPDF types so it is fully JVM
 * unit-testable. This is the most correctness-critical part of the engine:
 * text, signatures, OCR boxes, and covers must land on the exact PDF point.
 *
 * Coordinate systems:
 *  - PDF points: origin top-left, y grows downward, 1 pt = 1/72 in. (We use a
 *    top-left convention internally and convert to PDF's bottom-left origin
 *    only inside the exporter.)
 *  - Bitmap pixels: a page rendered at [renderScale] (pixels per PDF point).
 *  - Viewport pixels: the on-screen view after [zoom] and pan [panX]/[panY].
 *
 * Page rotation is the page's own /Rotate (0/90/180/270), applied before
 * scaling.
 */

data class PdfPointF(val x: Float, val y: Float)

data class PdfRectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun normalized(): PdfRectF = PdfRectF(
        minOf(left, right), minOf(top, bottom),
        maxOf(left, right), maxOf(top, bottom),
    )
}

/**
 * @param pageWidthPt  page width in PDF points (rotation already applied)
 * @param pageHeightPt page height in PDF points (rotation already applied)
 * @param renderScale  pixels per PDF point used when the page bitmap was drawn
 * @param zoom         user pinch zoom applied on top of the rendered bitmap
 * @param panX,panY    viewport translation in viewport pixels
 */
class PdfCoordinateMapper(
    val pageWidthPt: Float,
    val pageHeightPt: Float,
    val renderScale: Float,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    init {
        require(renderScale > 0f) { "renderScale must be > 0" }
        require(zoom > 0f) { "zoom must be > 0" }
    }

    /** Effective pixels-per-point currently shown on screen. */
    val effectiveScale: Float get() = renderScale * zoom

    fun pdfToBitmap(p: PdfPointF): PdfPointF =
        PdfPointF(p.x * renderScale, p.y * renderScale)

    fun bitmapToPdf(p: PdfPointF): PdfPointF =
        PdfPointF(p.x / renderScale, p.y / renderScale)

    fun pdfToViewport(p: PdfPointF): PdfPointF =
        PdfPointF(p.x * effectiveScale + panX, p.y * effectiveScale + panY)

    fun viewportToPdf(p: PdfPointF): PdfPointF =
        PdfPointF((p.x - panX) / effectiveScale, (p.y - panY) / effectiveScale)

    fun pdfRectToViewport(r: PdfRectF): PdfRectF {
        val tl = pdfToViewport(PdfPointF(r.left, r.top))
        val br = pdfToViewport(PdfPointF(r.right, r.bottom))
        return PdfRectF(tl.x, tl.y, br.x, br.y).normalized()
    }

    fun viewportRectToPdf(r: PdfRectF): PdfRectF {
        val tl = viewportToPdf(PdfPointF(r.left, r.top))
        val br = viewportToPdf(PdfPointF(r.right, r.bottom))
        return PdfRectF(tl.x, tl.y, br.x, br.y).normalized()
    }

    /** Clamp a PDF point inside the page bounds. */
    fun clampToPage(p: PdfPointF): PdfPointF = PdfPointF(
        p.x.coerceIn(0f, pageWidthPt),
        p.y.coerceIn(0f, pageHeightPt),
    )
}
