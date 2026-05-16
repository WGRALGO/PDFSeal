package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream
import java.io.OutputStream

/**
 * Thin wrapper over the Storage Access Framework. The engine reads/writes only
 * through this class so URI-permission handling lives in one place.
 */
class FileAccessManager(private val context: Context) {

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
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return c.getString(idx)
                }
            }
        return uri.lastPathSegment ?: "document.pdf"
    }

    fun readAllBytes(uri: Uri): ByteArray =
        openInput(uri).use { it.readBytes() }

    fun openInput(uri: Uri): InputStream =
        resolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")

    /** Open an output stream. "wt" truncates — only used on an explicit export target. */
    fun openOutput(uri: Uri, mode: String = "wt"): OutputStream =
        resolver.openOutputStream(uri, mode)
            ?: error("Cannot open output stream for $uri")
}
