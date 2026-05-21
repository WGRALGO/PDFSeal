package org.thewealthgapresolutionalgorithm.pdfseal.engine

import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.CoverReplaceObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrPageResult

/**
 * Turns OCR output into editable text overlays positioned in PDF points.
 *
 * Each OCR line becomes a [CoverReplaceObject]: a white background fill
 * that hides the original line, with a single [TextEditObject] painted on
 * top carrying the recognised text. Without the cover the original PDF
 * text would still show through behind the OCR text and the page would
 * read as garbled double-vision.
 *
 * This is OCR-based reconstruction, NOT native PDF text editing. The user
 * is expected to review/correct the text before export.
 */
object EditableCopyBuilder {

    fun buildOverlays(ocr: OcrPageResult): List<CoverReplaceObject> {
        if (ocr.renderedBitmapWidthPx == 0 || ocr.renderedBitmapHeightPx == 0) {
            return emptyList()
        }
        val sx = ocr.pdfPageWidthPt / ocr.renderedBitmapWidthPx
        val sy = ocr.pdfPageHeightPt / ocr.renderedBitmapHeightPx
        return ocr.boxes.map { box ->
            val r = box.boundsBitmapPx
            val coverRect = PdfRectF(
                r.left * sx, r.top * sy, r.right * sx, r.bottom * sy,
            ).normalized().padded(2f)
            val fontPt = (coverRect.height * 0.72f).coerceIn(6f, 96f)
            val text = TextEditObject(
                pageIndex = ocr.pageIndex,
                rectPt = coverRect,
                text = box.text.trim(),
                fontSizePt = fontPt,
            )
            CoverReplaceObject(
                pageIndex = ocr.pageIndex,
                rectPt = coverRect,
                fillArgb = 0xFFFFFFFF.toInt(),
                overlayText = mutableListOf(text),
            )
        }
    }

    private fun PdfRectF.padded(pt: Float): PdfRectF =
        PdfRectF(left - pt, top - pt, right + pt, bottom + pt)
}
