package org.thewealthgapresolutionalgorithm.pdfseal.engine.export

import android.content.Context
import android.net.Uri
import com.artifex.mupdf.fitz.PDFDocument
import org.thewealthgapresolutionalgorithm.pdfseal.engine.PdfDocumentSession
import org.thewealthgapresolutionalgorithm.pdfseal.engine.io.FileAccessManager
import java.io.File

/**
 * Exports an edited PDF **copy**. Never overwrites the source unless the caller
 * passes an explicit user-chosen target Uri with write permission.
 *
 * Phase 3 = honest skeleton: a faithful copy export works now. Flattening of
 * text / signature / cover / OCR edits is implemented in Phase 4 — until then
 * exporting a session that has edits throws, rather than silently dropping
 * them (no fake "done" features).
 */
class PdfExporter(
    private val context: Context,
    private val files: FileAccessManager,
) {
    class FlatteningNotYetImplemented(msg: String) : UnsupportedOperationException(msg)

    /**
     * Write [session] to [targetUri] (a SAF document the user just created).
     * @return the targetUri on success.
     */
    fun exportCopy(session: PdfDocumentSession, targetUri: Uri): Uri {
        if (session.hasUnsavedEdits) {
            throw FlatteningNotYetImplemented(
                "Edit flattening (text/signature/cover/OCR) is implemented in " +
                    "Phase 4. Refusing to export and silently lose edits.",
            )
        }
        val doc = session.document
        val pdf = doc as? PDFDocument
            ?: throw IllegalStateException("Source is not a PDF document.")

        // MuPDF saves to a path; stage to cache then stream to the SAF target.
        val temp = File.createTempFile("pdfseal_export_", ".pdf", context.cacheDir)
        try {
            pdf.save(temp.absolutePath, "")
            files.openOutput(targetUri, "wt").use { out ->
                temp.inputStream().use { it.copyTo(out) }
            }
        } finally {
            temp.delete()
        }
        return targetUri
    }
}
