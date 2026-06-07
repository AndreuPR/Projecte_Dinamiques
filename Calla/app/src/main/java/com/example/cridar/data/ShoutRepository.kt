package com.example.cridar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shout_settings")

class ShoutRepository(private val context: Context) {

    companion object {
        private val MIN_DB = floatPreferencesKey("min_db")
        private val MAX_DB = floatPreferencesKey("max_db")
        private val REFRESH_MS = intPreferencesKey("refresh_ms")
        private val THRESHOLD = floatPreferencesKey("threshold") // 0-100
        private val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
    }

    val minDb: Flow<Float> = context.dataStore.data.map { it[MIN_DB] ?: -70f }
    val maxDb: Flow<Float> = context.dataStore.data.map { it[MAX_DB] ?: 0f }
    val refreshMs: Flow<Int> = context.dataStore.data.map { it[REFRESH_MS] ?: 200 }
    val threshold: Flow<Float> = context.dataStore.data.map { it[THRESHOLD] ?: 75f } // per sobre de 75 es considera crit
    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { it[VOICE_ENABLED] ?: false }

    suspend fun setMinDb(value: Float) { context.dataStore.edit { it[MIN_DB] = value } }
    suspend fun setMaxDb(value: Float) { context.dataStore.edit { it[MAX_DB] = value } }
    suspend fun setRefreshMs(value: Int) { context.dataStore.edit { it[REFRESH_MS] = value } }
    suspend fun setThreshold(value: Float) { context.dataStore.edit { it[THRESHOLD] = value } }
    suspend fun setVoiceEnabled(value: Boolean) { context.dataStore.edit { it[VOICE_ENABLED] = value } }
}