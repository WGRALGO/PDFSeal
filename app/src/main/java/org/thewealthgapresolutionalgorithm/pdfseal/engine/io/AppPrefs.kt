package org.thewealthgapresolutionalgorithm.pdfseal.engine.io

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "pdfseal_app")

/**
 * Small local-only app state. Currently just the one-time acknowledgement of
 * the first-launch limits screen. No network, no account — DataStore on this
 * device only.
 */
class AppPrefs(private val context: Context) {

    private val limitsAckKey = booleanPreferencesKey("limits_acknowledged_v1")

    val limitsAcknowledged: Flow<Boolean> =
        context.appDataStore.data.map { it[limitsAckKey] ?: false }

    suspend fun setLimitsAcknowledged() {
        context.appDataStore.edit { it[limitsAckKey] = true }
    }
}
