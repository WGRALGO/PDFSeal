package org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr

import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF

/** One recognised text box with its bitmap-space bounds and confidence. */
data class OcrBox(
    val text: String,
    val boundsBitmapPx: PdfRectF,
    val confidence: Float,
    val level: Level,
) {
    enum class Level { BLOCK, PARA, LINE, WORD }
}

/**
 * OCR output for a single page. Keeps both the rendered bitmap size and the
 * PDF page size so [OcrBox] bitmap coordinates can be mapped back to PDF
 * points later (via PdfCoordinateMapper) without re-running OCR.
 */
data class OcrPageResult(
    val pageIndex: Int,
    val fullText: String,
    val boxes: List<OcrBox>,
    val renderedBitmapWidthPx: Int,
    val renderedBitmapHeightPx: Int,
    val pdfPageWidthPt: Float,
    val pdfPageHeightPt: Float,
    val language: String,
    val meanConfidence: Float,
)
