package com.example.dinamiqapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
            DynamicLevel.PP to DynamicRange(-65f, -55f),
            DynamicLevel.P  to DynamicRange(-55f, -48f),
            DynamicLevel.MF to DynamicRange(-48f, -40f),
            DynamicLevel.F  to DynamicRange(-40f, -32f),
            DynamicLevel.FF to DynamicRange(-32f, -18f),
        )
    )
    val CONCERT = DynamicsProfile(
        name = "Concert",
        ranges = mapOf(
            DynamicLevel.PP to DynamicRange(-80f, -70f),
            DynamicLevel.P  to DynamicRange(-70f, -62f),
            DynamicLevel.MF to DynamicRange(-62f, -54f),
            DynamicLevel.F  to DynamicRange(-54f, -44f),
            DynamicLevel.FF to DynamicRange(-44f, -25f),
        )
    )
}

// ──────────────────────────────────────────────
// DataStore persistence
// ──────────────────────────────────────────────

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dynamics_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        // Keys for each dynamic min/max per profile
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

    suspend fun saveRange(profileName: String, level: DynamicLevel, range: DynamicRange) {
        context.dataStore.edit { prefs ->
            prefs[rangeKey(profileName, level, true)]  = range.min
            prefs[rangeKey(profileName, level, false)] = range.max
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