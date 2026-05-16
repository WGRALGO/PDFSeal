package org.thewealthgapresolutionalgorithm.pdfseal.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCoordinateMapperTest {

    private val eps = 1e-3f

    private fun mapper(
        scale: Float = 2f,
        zoom: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f,
    ) = PdfCoordinateMapper(
        pageWidthPt = 612f,
        pageHeightPt = 792f,
        renderScale = scale,
        zoom = zoom,
        panX = panX,
        panY = panY,
    )

    @Test fun pdfToBitmapScales() {
        val m = mapper(scale = 2f)
        val b = m.pdfToBitmap(PdfPointF(100f, 50f))
        assertEquals(200f, b.x, eps)
        assertEquals(100f, b.y, eps)
    }

    @Test fun bitmapToPdfIsInverseOfPdfToBitmap() {
        val m = mapper(scale = 3f)
        val p = PdfPointF(123f, 456f)
        val round = m.bitmapToPdf(m.pdfToBitmap(p))
        assertEquals(p.x, round.x, eps)
        assertEquals(p.y, round.y, eps)
    }

    @Test fun viewportRoundTripWithZoomAndPan() {
        val m = mapper(scale = 2f, zoom = 1.5f, panX = 40f, panY = -25f)
        val p = PdfPointF(300f, 220f)
        val round = m.viewportToPdf(m.pdfToViewport(p))
        assertEquals(p.x, round.x, eps)
        assertEquals(p.y, round.y, eps)
    }

    @Test fun effectiveScaleIsRenderTimesZoom() {
        assertEquals(3f, mapper(scale = 2f, zoom = 1.5f).effectiveScale, eps)
    }

    @Test fun pdfRectToViewportNormalises() {
        val m = mapper(scale = 1f, zoom = 1f)
        val r = m.pdfRectToViewport(PdfRectF(10f, 20f, 110f, 220f))
        assertEquals(10f, r.left, eps)
        assertEquals(20f, r.top, eps)
        assertEquals(110f, r.right, eps)
        assertEquals(220f, r.bottom, eps)
    }

    @Test fun clampKeepsPointInsidePage() {
        val m = mapper()
        val c = m.clampToPage(PdfPointF(-50f, 10_000f))
        assertEquals(0f, c.x, eps)
        assertEquals(792f, c.y, eps)
    }
}
