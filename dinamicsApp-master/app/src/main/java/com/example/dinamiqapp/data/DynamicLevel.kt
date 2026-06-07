package com.example.dinamiqapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ──────────────────────────────────────────────
// Data models
// ──────────────────────────────────────────────

enum class DynamicLevel(val symbol: String, val fullName: String) {
    PP("pp", "Pianissimo"),
    P("p", "Piano"),
    MF("mf", "Mezzof."),
    F("f", "Forte"),
    FF("ff", "Fortissimo");
}

data class DynamicRange(val min: Float, val max: Float)

data class DynamicsProfile(
    val name: String,
    val ranges: Map<DynamicLevel, DynamicRange>
)

data class AudioReading(
    val db: Float,
    val level: DynamicLevel?,
    // 0..1: how centred within the range (1 = perfectly centred)
    val precision: Float
)

// ──────────────────────────────────────────────
// Default profiles
// ──────────────────────────────────────────────

object DefaultProfiles {
    val HOME = DynamicsProfile(
        name = "Casa",
        ranges = mapOf(
            DynamicLevel.PP to DynamicRange(ScaleConverter.scaleToDb(38f), ScaleConverter.scaleToDb(70f)),
            DynamicLevel.P  to DynamicRange(ScaleConverter.scaleToDb(71f), ScaleConverter.scaleToDb(74f)),
            DynamicLevel.MF to DynamicRange(ScaleConverter.scaleToDb(75f), ScaleConverter.scaleToDb(78f)),
            DynamicLevel.F  to DynamicRange(ScaleConverter.scaleToDb(79f), ScaleConverter.scaleToDb(81f)),
            DynamicLevel.FF to DynamicRange(ScaleConverter.scaleToDb(82f), ScaleConverter.scaleToDb(100f)),
        )
    )
    val CONCERT = DynamicsProfile(
        name = "Concert",
        ranges = mapOf(
            DynamicLevel.PP to DynamicRange(ScaleConverter.scaleToDb(40f), ScaleConverter.scaleToDb(72f)),
            DynamicLevel.P  to DynamicRange(ScaleConverter.scaleToDb(73f), ScaleConverter.scaleToDb(76f)),
            DynamicLevel.MF to DynamicRange(ScaleConverter.scaleToDb(77f), ScaleConverter.scaleToDb(79f)),
            DynamicLevel.F  to DynamicRange(ScaleConverter.scaleToDb(80f), ScaleConverter.scaleToDb(82f)),
            DynamicLevel.FF to DynamicRange(ScaleConverter.scaleToDb(83f), ScaleConverter.scaleToDb(100f)),
        )
    )
}

// ──────────────────────────────────────────────
// DataStore persistence
// ──────────────────────────────────────────────

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dynamics_settings")

class SettingsRepository(private val context: Context) {
    val appMinDb: Flow<Float> = context.dataStore.data.map { it[APP_MIN_DB] ?: -70f }
    val appMaxDb: Flow<Float> = context.dataStore.data.map { it[APP_MAX_DB] ?: 0f }
    val appRefreshMs: Flow<Int> = context.dataStore.data.map { it[APP_REFRESH_MS] ?: 200 }

    companion object {
        private val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        private val APP_MIN_DB = floatPreferencesKey("app_min_db")
        private val APP_MAX_DB = floatPreferencesKey("app_max_db")
        private val APP_REFRESH_MS = intPreferencesKey("app_refresh_ms")
        private fun rangeKey(profile: String, level: DynamicLevel, isMin: Boolean) =
            floatPreferencesKey("${profile}_${level.name}_${if (isMin) "min" else "max"}")
    }

    val activeProfileName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROFILE] ?: "Casa"
    }

    fun profileRanges(profileName: String): Flow<Map<DynamicLevel, DynamicRange>> =
        context.dataStore.data.map { prefs ->
            val defaults = if (profileName == "Concert") DefaultProfiles.CONCERT else DefaultProfiles.HOME
            DynamicLevel.values().associateWith { level ->
                val min = prefs[rangeKey(profileName, level, true)]
                    ?: defaults.ranges[level]!!.min
                val max = prefs[rangeKey(profileName, level, false)]
                    ?: defaults.ranges[level]!!.max
                DynamicRange(min, max)
            }
        }

    suspend fun saveActiveProfile(name: String) {
        context.dataStore.edit { it[ACTIVE_PROFILE] = name }
    }
    suspend fun setAppMinDb(value: Float) {
        context.dataStore.edit { it[APP_MIN_DB] = value }
    }
    suspend fun setAppMaxDb(value: Float) {
        context.dataStore.edit { it[APP_MAX_DB] = value }
    }
    suspend fun setAppRefreshMs(value: Int) {
        context.dataStore.edit { it[APP_REFRESH_MS] = value }
    }

    suspend fun saveRange(profileName: String, level: DynamicLevel, range: DynamicRange) {
        context.dataStore.edit { prefs ->
            prefs[rangeKey(profileName, level, true)]  = range.min
            prefs[rangeKey(profileName, level, false)] = range.max
        }
    }

    // Aquesta és la funció nova, ben col·locada dins la classe
    suspend fun resetToDefaults(profileName: String) {
        val defaults = if (profileName == "Concert") DefaultProfiles.CONCERT else DefaultProfiles.HOME
        defaults.ranges.forEach { (level, range) ->
            saveRange(profileName, level, range)
        }
    }
}

// ──────────────────────────────────────────────
// Audio → dB → DynamicLevel mapping
// ──────────────────────────────────────────────

fun classifyDb(db: Float, ranges: Map<DynamicLevel, DynamicRange>): AudioReading {
    for (level in DynamicLevel.values()) {
        val range = ranges[level] ?: continue
        if (db >= range.min && db < range.max) {
            val centre = (range.min + range.max) / 2f
            val halfWidth = (range.max - range.min) / 2f
            val precision = 1f - (kotlin.math.abs(db - centre) / halfWidth)
            return AudioReading(db, level, precision.coerceIn(0f, 1f))
        }
    }
    return AudioReading(db, null, 0f)
}