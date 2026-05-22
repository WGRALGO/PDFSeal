package org.thewealthgapresolutionalgorithm.pdfseal.engine.edit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import org.thewealthgapresolutionalgorithm.pdfseal.R

/**
 * Single source of truth for turning a [PdfEditObject] into pixels.
 *
 * All draw calls work in **PDF-point space** (1 unit = 1 pt). The caller is
 * responsible for any scale: the exporter draws onto a PdfDocument canvas whose
 * units already are points (scale 1), the on-screen overlay pre-scales its
 * canvas by the render scale. Because both paths go through this one class the
 * on-screen preview is guaranteed to match the exported file (this was the root
 * cause of the "signature/font size doesn't work" bugs — the preview used a
 * fixed Compose label instead of this paint logic).
 */
class EditObjectPainter(private val context: Context) {

    private val typefaceCache = HashMap<Int, Typeface>()

    private fun signatureFontRes(style: SignatureEditObject.SignatureStyle): Int =
        when (style) {
            SignatureEditObject.SignatureStyle.ELEGANT_CURSIVE -> R.font.great_vibes
            SignatureEditObject.SignatureStyle.BOLD_HANDWRITTEN -> R.font.pacifico
            SignatureEditObject.SignatureStyle.CLEAN_FORMAL_SCRIPT ->
                R.font.pinyon_script
        }

    private fun typeface(fontRes: Int): Typeface =
        typefaceCache.getOrPut(fontRes) {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        }

    /** Paint a single edit object in point space. */
    fun draw(canvas: Canvas, obj: PdfEditObject) {
        when (obj) {
            is CoverReplaceObject -> {
                val r = obj.rectPt
                canvas.drawRect(
                    RectF(r.left, r.top, r.right, r.bottom),
                    Paint().apply {
                        color = obj.fillArgb
                        style = Paint.Style.FILL
                    },
                )
                obj.overlayText.forEach { drawText(canvas, it) }
            }
            is TextEditObject -> drawText(canvas, obj)
            is SignatureEditObject -> drawSignature(canvas, obj)
            is HighlightObject -> {
                val r = obj.rectPt
                canvas.drawRect(
                    RectF(r.left, r.top, r.right, r.bottom),
                    Paint().apply {
                        color = obj.colorArgb
                        style = Paint.Style.FILL
                    },
                )
            }
            is StrikethroughObject -> {
                val r = obj.rectPt
                val midY = (r.top + r.bottom) / 2f
                canvas.drawLine(
                    r.left, midY, r.right, midY,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = obj.colorArgb
                        strokeWidth = obj.thicknessPt
                    },
                )
            }
        }
    }

    fun drawText(canvas: Canvas, t: TextEditObject) {
        if (t.text.isEmpty()) return
        val base = when (t.fontFamily) {
            "Serif" -> Typeface.SERIF
            "Mono" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val style = when {
            t.bold && t.italic -> Typeface.BOLD_ITALIC
            t.bold -> Typeface.BOLD
            t.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.colorArgb
            // Font scales with the box height (same model as signatures), so
            // dragging a corner resizes the text and the box always fits it.
            textSize = t.rectPt.height.coerceIn(6f, 400f) * 0.8f
            typeface = Typeface.create(base, style)
            textAlign = when (t.alignment) {
                TextEditObject.TextAlign.LEFT -> Paint.Align.LEFT
                TextEditObject.TextAlign.CENTER -> Paint.Align.CENTER
                TextEditObject.TextAlign.RIGHT -> Paint.Align.RIGHT
            }
        }
        val r = t.rectPt
        val x = when (t.alignment) {
            TextEditObject.TextAlign.LEFT -> r.left
            TextEditObject.TextAlign.CENTER -> (r.left + r.right) / 2f
            TextEditObject.TextAlign.RIGHT -> r.right
        }
        canvas.save()
        if (t.rotationDeg != 0f) canvas.rotate(t.rotationDeg, r.left, r.top)
        // Baseline ≈ top + ascent (ascent is negative).
        canvas.drawText(t.text, x, r.top - paint.ascent(), paint)
        canvas.restore()
    }

    fun drawSignature(canvas: Canvas, s: SignatureEditObject) {
        if (s.typedName.isEmpty()) return
        val r = s.rectPt
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.colorArgb
            typeface = typeface(signatureFontRes(s.style))
            textSize = r.height.coerceIn(8f, 400f) * 0.8f
        }
        canvas.save()
        if (s.rotationDeg != 0f) canvas.rotate(s.rotationDeg, r.left, r.top)
        canvas.drawText(s.typedName, r.left, r.top - paint.ascent(), paint)
        canvas.restore()
    }
}
