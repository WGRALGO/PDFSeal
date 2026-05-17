package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Thin wrapper over the Storage Access Framework. The engine reads/writes only
 * through this class so URI-permission handling lives in one place.
 */
class FileAccessManager(private val context: Context) {

    companion object {
        /** Max wall time to pull a content URI into cache before failing. */
        const val OPEN_TIMEOUT_MS = 30_000L

        /** Max wall time for the non-critical display-name lookup. */
        const val NAME_TIMEOUT_MS = 8_000L
    }

    private val resolver: ContentResolver get() = context.contentResolver

    fun takePersistableRead(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    fun takePersistableReadWrite(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    fun hasPermission(uri: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    fun displayName(uri: Uri): String {
        val fallback = uri.lastPathSegment ?: "document.pdf"
        // The title is non-critical, but resolver.query() can itself hang on a
        // bad provider — bound it and fall back rather than block the open.
        return runCatching {
            IoWatchdog.callWithTimeout(NAME_TIMEOUT_MS) {
                resolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) c.getString(idx) else null
                    } else {
                        null
                    }
                }
            }
        }.getOrNull() ?: fallback
    }

    fun openInput(uri: Uri): InputStream =
        resolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")

    /**
     * Stream the content URI into a private temp file under the app cache (a
     * no-backup, auto-purgeable area) instead of reading the whole PDF into a
     * ByteArray. MuPDF then opens it by path and memory-maps lazily, so large
     * PDFs no longer have to fit in RAM. Caller owns the returned file and must
     * delete it (see PdfDocumentSession.close()).
     */
    /**
     * Best-effort sweep of temp PDFs left behind by a previous crash/kill (the
     * normal path deletes them in [PdfDocumentSession.close]). Only files older
     * than [maxAgeMs] are removed so a concurrently-open document is never
     * pulled out from under MuPDF.
     */
    fun purgeStaleTempFiles(maxAgeMs: Long = 60 * 60 * 1000L) {
        val dir = File(context.cacheDir, "pdfseal_open")
        val cutoff = System.currentTimeMillis() - maxAgeMs
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.lastModified() < cutoff) runCatching { f.delete() }
        }
    }

    fun copyToCacheTempFile(uri: Uri, timeoutMs: Long = OPEN_TIMEOUT_MS): File {
        val dir = File(context.cacheDir, "pdfseal_open").apply { mkdirs() }
        val temp = File.createTempFile("doc_", ".pdf", dir)
        try {
            // openInputStream() itself — not just the read — can hang
            // indefinitely on a Fire OS media-store / remote content:// URI
            // (confirmed on-device: "Loading…" forever, no error). Run the
            // whole intake under the watchdog so a stuck provider call is
            // abandoned and surfaces a clear error instead.
            IoWatchdog.callWithTimeout(timeoutMs) {
                val input = resolver.openInputStream(uri)
                    ?: throw java.io.IOException(
                        "Could not read the file. The app may have lost " +
                            "access to it — reopen it from your file manager.",
                    )
                input.use { ins ->
                    temp.outputStream().use { outs -> ins.copyTo(outs, 64 * 1024) }
                }
            }
        } catch (e: SecurityException) {
            temp.delete()
            throw java.io.IOException(
                "Permission to this file was lost. Reopen it from your file " +
                    "manager.",
                e,
            )
        } catch (e: Throwable) {
            temp.delete()
            throw e
        }
        return temp
    }

    /** Open an output stream. "wt" truncates — only used on an explicit export target. */
    fun openOutput(uri: Uri, mode: String = "wt"): OutputStream =
        resolver.openOutputStream(uri, mode)
            ?: error("Cannot open output stream for $uri")
}
