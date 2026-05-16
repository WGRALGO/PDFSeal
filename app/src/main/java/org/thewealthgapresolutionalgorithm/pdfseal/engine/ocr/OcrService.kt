package org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import java.io.File

class OcrUnavailableException(message: String) : Exception(message)

/**
 * Offline OCR via Tesseract4Android. No network, no cloud.
 *
 * Trained data (`eng.traineddata`) is bundled in assets/tessdata and copied to
 * filesDir on first use. Until the OCR phase bundles it, [recognizePage] fails
 * loudly with [OcrUnavailableException] — it never returns fake results.
 */
class OcrService(private val context: Context) {

    private val tessRoot: File get() = File(context.filesDir, "tess")
    private val tessdataDir: File get() = File(tessRoot, "tessdata")

    private fun ensureTrainedData(language: String) {
        val target = File(tessdataDir, "$language.traineddata")
        if (target.exists() && target.length() > 0) return
        tessdataDir.mkdirs()
        val assetPath = "tessdata/$language.traineddata"
        val available = runCatching { context.assets.list("tessdata")?.toList() }
            .getOrNull().orEmpty()
        if (assetPath.substringAfterLast('/') !in available) {
            throw OcrUnavailableException(
                "OCR language '$language' not bundled. Expected asset " +
                    "'$assetPath'. (Bundled in the OCR phase — see ROADMAP.)",
            )
        }
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
    }

    /**
     * Recognise [bitmap] (a page rendered at high DPI). Bitmap-space boxes are
     * returned so the caller can map them to PDF points.
     */
    fun recognizePage(
        bitmap: Bitmap,
        pageIndex: Int,
        pdfPageWidthPt: Float,
        pdfPageHeightPt: Float,
        language: String = "eng",
    ): OcrPageResult {
        ensureTrainedData(language)
        val api = TessBaseAPI()
        if (!api.init(tessRoot.absolutePath, language)) {
            api.recycle()
            throw OcrUnavailableException("Tesseract init failed for '$language'.")
        }
        try {
            api.setImage(bitmap)
            val fullText = api.utF8Text ?: ""
            val boxes = ArrayList<OcrBox>()
            val it = api.resultIterator
            if (it != null) {
                val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                it.begin()
                do {
                    val r = it.getBoundingRect(level) ?: continue
                    val txt = it.getUTF8Text(level) ?: ""
                    if (txt.isBlank()) continue
                    boxes.add(
                        OcrBox(
                            text = txt,
                            boundsBitmapPx = PdfRectF(
                                r.left.toFloat(), r.top.toFloat(),
                                r.right.toFloat(), r.bottom.toFloat(),
                            ),
                            confidence = it.confidence(level),
                            level = OcrBox.Level.LINE,
                        ),
                    )
                } while (it.next(level))
            }
            return OcrPageResult(
                pageIndex = pageIndex,
                fullText = fullText,
                boxes = boxes,
                renderedBitmapWidthPx = bitmap.width,
                renderedBitmapHeightPx = bitmap.height,
                pdfPageWidthPt = pdfPageWidthPt,
                pdfPageHeightPt = pdfPageHeightPt,
                language = language,
                meanConfidence = api.meanConfidence().toFloat(),
            )
        } finally {
            api.recycle()
        }
    }
}
