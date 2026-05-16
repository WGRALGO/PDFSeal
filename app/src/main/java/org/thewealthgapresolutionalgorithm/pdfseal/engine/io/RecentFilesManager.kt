package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentDataStore by preferencesDataStore(name = "pdfseal_recent")

data class RecentFile(val uri: String, val displayName: String, val lastOpenedMs: Long)

/**
 * Recent documents, persisted via DataStore. Entries whose SAF permission was
 * revoked are filtered out by the caller via [FileAccessManager.hasPermission].
 *
 * Stored as newline-separated `uri\tname\tmillis` records — no extra
 * serialization dependency for a list this small.
 */
class RecentFilesManager(private val context: Context) {

    private val key = stringPreferencesKey("recent_v1")
    private val maxEntries = 20

    val recent: Flow<List<RecentFile>> =
        context.recentDataStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun add(uri: String, displayName: String) {
        context.recentDataStore.edit { prefs ->
            val current = decode(prefs[key]).filterNot { it.uri == uri }
            val updated = (listOf(
                RecentFile(uri, displayName, System.currentTimeMillis()),
            ) + current).take(maxEntries)
            prefs[key] = encode(updated)
        }
    }

    suspend fun remove(uri: String) {
        context.recentDataStore.edit { prefs ->
            prefs[key] = encode(decode(prefs[key]).filterNot { it.uri == uri })
        }
    }

    private fun encode(list: List<RecentFile>): String =
        list.joinToString("\n") { "${it.uri}\t${it.displayName}\t${it.lastOpenedMs}" }

    private fun decode(raw: String?): List<RecentFile> =
        raw?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { line ->
                val p = line.split("\t")
                if (p.size == 3) {
                    RecentFile(p[0], p[1], p[2].toLongOrNull() ?: 0L)
                } else {
                    null
                }
            }
            ?.toList()
            .orEmpty()
}
