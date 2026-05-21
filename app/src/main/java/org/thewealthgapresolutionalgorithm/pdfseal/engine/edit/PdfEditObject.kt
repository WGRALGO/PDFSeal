package org.thewealthgapresolutionalgorithm.pdfseal.engine.edit

import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import java.util.UUID

/**
 * Base model for anything the user places on a PDF page. Geometry is stored in
 * PDF points so it survives zoom/pan; the UI maps via PdfCoordinateMapper.
 *
 * These are plain models — they hold no MuPDF references. The exporter is what
 * turns them into PDF content on save (flattening).
 */
sealed class PdfEditObject {
    abstract val id: String
    abstract val pageIndex: Int
    abstract var rectPt: PdfRectF
    abstract var rotationDeg: Float
    abstract var zOrder: Int
    var selected: Boolean = false
}

data class TextEditObject(
    override val id: String = UUID.randomUUID().toString(),
    override val pageIndex: Int,
    override var rectPt: PdfRectF,
    override var rotationDeg: Float = 0f,
    override var zOrder: Int = 0,
    var text: String = "",
    var fontFamily: String = "Sans",
    var fontSizePt: Float = 12f,
    var colorArgb: Int = 0xFF000000.toInt(),
    var alignment: TextAlign = TextAlign.LEFT,
    var bold: Boolean = false,
    var italic: Boolean = false,
) : PdfEditObject() {
    enum class TextAlign { LEFT, CENTER, RIGHT }
}

data class SignatureEditObject(
    override val id: String = UUID.randomUUID().toString(),
    override val pageIndex: Int,
    override var rectPt: PdfRectF,
    override var rotationDeg: Float = 0f,
    override var zOrder: Int = 0,
    var typedName: String = "",
    var style: SignatureStyle = SignatureStyle.ELEGANT_CURSIVE,
    var colorArgb: Int = 0xFF101010.toInt(),
) : PdfEditObject() {
    /** Visual typed-name signature only — NOT a certified/cryptographic signature. */
    enum class SignatureStyle { ELEGANT_CURSIVE, BOLD_HANDWRITTEN, CLEAN_FORMAL_SCRIPT }
}

/**
 * Visual cover + optional replacement text on top.
 *
 * THIS IS NOT SECURE REDACTION. Covered content may persist in the exported
 * file unless the page is rasterised. Never label this redaction.
 */
data class CoverReplaceObject(
    override val id: String = UUID.randomUUID().toString(),
    override val pageIndex: Int,
    override var rectPt: PdfRectF,
    override var rotationDeg: Float = 0f,
    override var zOrder: Int = 0,
    var fillArgb: Int = 0xFFFFFFFF.toInt(),
    val overlayText: MutableList<TextEditObject> = mutableListOf(),
) : PdfEditObject()

/**
 * Translucent yellow rectangle drawn over a region. Purely visual — never
 * a substitute for redaction (the underlying content is still in the PDF).
 */
data class HighlightObject(
    override val id: String = UUID.randomUUID().toString(),
    override val pageIndex: Int,
    override var rectPt: PdfRectF,
    override var rotationDeg: Float = 0f,
    override var zOrder: Int = 0,
    var colorArgb: Int = 0x66FFEB3B.toInt(),
) : PdfEditObject()

/**
 * Horizontal line painted through the vertical centre of [rectPt] to
 * cross out underlying text. Visual only — original glyphs remain in the
 * file.
 */
data class StrikethroughObject(
    override val id: String = UUID.randomUUID().toString(),
    override val pageIndex: Int,
    override var rectPt: PdfRectF,
    override var rotationDeg: Float = 0f,
    override var zOrder: Int = 0,
    var colorArgb: Int = 0xFF000000.toInt(),
    var thicknessPt: Float = 1.5f,
) : PdfEditObject()
