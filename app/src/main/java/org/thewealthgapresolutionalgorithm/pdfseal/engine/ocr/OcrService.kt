package org.thewealthgapresolutionalgorithm.pdfseal.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfRectF
import java.io.File
import java.io.FileOutputStream

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
        // Tesseract4Android setImage(Bitmap) expects opaque RGBA. The renderer
        // creates ARGB_8888 bitmaps where hasAlpha=true even though every
        // pixel is fully opaque (eraseColor(WHITE) + MuPDF draw). Some devices
        // feed the native side premultiplied/transparent garbage in that
        // state → blank OCR. Flatten onto a fresh opaque bitmap.
        val ocrBitmap = if (!bitmap.hasAlpha()) bitmap else {
            val flat = Bitmap.createBitmap(
                bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888,
            )
            flat.eraseColor(Color.WHITE)
            Canvas(flat).drawBitmap(bitmap, 0f, 0f, null)
            flat.setHasAlpha(false)
            flat
        }
        runCatching {
            val dumpFile = File(context.filesDir, "last-ocr-input.png")
            FileOutputStream(dumpFile).use {
                ocrBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Log.d(
                "PdfSeal",
                "OCR input dumped to ${dumpFile.absolutePath} " +
                    "size=${dumpFile.length()} dims=${ocrBitmap.width}x${ocrBitmap.height} " +
                    "hasAlpha=${ocrBitmap.hasAlpha()} sourceHadAlpha=${bitmap.hasAlpha()}",
            )
        }
        val api = TessBaseAPI()
        if (!api.init(tessRoot.absolutePath, language)) {
            api.recycle()
            throw OcrUnavailableException("Tesseract init failed for '$language'.")
        }
        try {
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
            api.setImage(ocrBitmap)
            val fullText = api.utF8Text ?: ""
            Log.d(
                "PdfSeal",
                "OCR recognize page=$pageIndex lang=$language " +
                    "psm=${api.pageSegMode} meanConf=${api.meanConfidence()} " +
                    "fullTextLen=${fullText.length}",
            )
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
            Log.d(
                "PdfSeal",
                "OCR result page=$pageIndex lines=${boxes.size} " +
                    "fullTextPreview='${fullText.take(120).replace("\n", "\\n")}'",
            )
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
