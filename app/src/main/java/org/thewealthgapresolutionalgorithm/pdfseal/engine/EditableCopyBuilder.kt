package org.thewealthgapresolutionalgorithm.pdfseal.engine

import org.thewealthgapresolutionalgorithm.pdfseal.engine.edit.TextEditObject
import org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr.OcrPageResult

/**
 * Turns OCR output into editable text overlays positioned in PDF points.
 *
 * This is OCR-based reconstruction, NOT native PDF text editing. The user is
 * expected to review/correct the text before export.
 */
object EditableCopyBuilder {

    fun buildOverlays(ocr: OcrPageResult): List<TextEditObject> {
        if (ocr.renderedBitmapWidthPx == 0 || ocr.renderedBitmapHeightPx == 0) {
            return emptyList()
        }
        val sx = ocr.pdfPageWidthPt / ocr.renderedBitmapWidthPx
        val sy = ocr.pdfPageHeightPt / ocr.renderedBitmapHeightPx
        return ocr.boxes.map { box ->
            val r = box.boundsBitmapPx
            val rectPt = PdfRectF(
                r.left * sx, r.top * sy, r.right * sx, r.bottom * sy,
            ).normalized()
            // Approximate font size from the line box height in points.
            val fontPt = (rectPt.height * 0.8f).coerceIn(6f, 96f)
            TextEditObject(
                pageIndex = ocr.pageIndex,
                rectPt = rectPt,
                text = box.text.trim(),
                fontSizePt = fontPt,
            )
        }
    }
}
