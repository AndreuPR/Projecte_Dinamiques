package com.example.dinamiqapp.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

data class VoiceProfile(
    val id: String,
    val name: String,
    val mfccCentroid: FloatArray,
    val sampleCount: Int
)

class VoiceRepository(private val context: Context) {

    companion object {
        private val VOICE_LIST      = stringPreferencesKey("voice_list")
        private val ACTIVE_VOICE_ID = stringPreferencesKey("active_voice_id")
        private val KEEP_LEARNING   = booleanPreferencesKey("keep_learning")

        private fun nameKey(id: String)    = stringPreferencesKey("voice_${id}_name")
        private fun mfccKey(id: String)    = stringPreferencesKey("voice_${id}_mfcc")
        private fun samplesKey(id: String) = intPreferencesKey("voice_${id}_samples")
    }

    val activeVoiceId: Flow<String>       = context.dataStore.data.map { it[ACTIVE_VOICE_ID] ?: "" }
    val keepLearning: Flow<Boolean>       = context.dataStore.data.map { it[KEEP_LEARNING] ?: false }

    val allProfiles: Flow<List<VoiceProfile>> = context.dataStore.data.map { prefs ->
        idsFromPrefs(prefs).mapNotNull { id -> loadProfile(prefs, id) }
    }

    suspend fun activeProfile(): VoiceProfile? {
        val prefs = context.dataStore.data.first()
        val id = prefs[ACTIVE_VOICE_ID] ?: return null
        return if (id.isBlank()) null else loadProfile(prefs, id)
    }

    suspend fun setActiveVoice(id: String) {
        context.dataStore.edit { it[ACTIVE_VOICE_ID] = id }
    }

    suspend fun clearActiveVoice() {
        context.dataStore.edit { it[ACTIVE_VOICE_ID] = "" }
    }

    suspend fun setKeepLearning(value: Boolean) {
        context.dataStore.edit { it[KEEP_LEARNING] = value }
    }

    suspend fun saveProfile(profile: VoiceProfile) {
        context.dataStore.edit { prefs ->
            val ids = idsFromPrefs(prefs).toMutableList()
            if (profile.id !in ids) ids.add(profile.id)
            prefs[VOICE_LIST]          = ids.joinToString(",")
            prefs[nameKey(profile.id)] = profile.name
            prefs[mfccKey(profile.id)] = profile.mfccCentroid.joinToString(",")
            prefs[samplesKey(profile.id)] = profile.sampleCount
        }
    }

    suspend fun deleteProfile(id: String) {
        context.dataStore.edit { prefs ->
            val ids = idsFromPrefs(prefs).filter { it != id }
            prefs[VOICE_LIST] = ids.joinToString(",")
            prefs.remove(nameKey(id))
            prefs.remove(mfccKey(id))
            prefs.remove(samplesKey(id))
            if (prefs[ACTIVE_VOICE_ID] == id) prefs[ACTIVE_VOICE_ID] = ""
        }
    }

    /** Fusiona un nou centroid MFCC amb l'existent per a aprenentatge progressiu */
    suspend fun improveProfile(id: String, newMfcc: FloatArray) {
        context.dataStore.edit { prefs ->
            val oldMfccStr = prefs[mfccKey(id)] ?: return@edit
            val oldCount   = prefs[samplesKey(id)] ?: 1
            val oldMfcc    = parseMfcc(oldMfccStr) ?: return@edit
            if (oldMfcc.size != newMfcc.size) return@edit

            val newCount = oldCount + 1
            val blended  = FloatArray(oldMfcc.size) { i ->
                (oldMfcc[i] * oldCount + newMfcc[i]) / newCount
            }
            prefs[mfccKey(id)]       = blended.joinToString(",")
            prefs[samplesKey(id)]    = newCount
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun idsFromPrefs(prefs: Preferences): List<String> =
        (prefs[VOICE_LIST] ?: "").split(",").filter { it.isNotBlank() }

    private fun loadProfile(prefs: Preferences, id: String): VoiceProfile? {
        val name    = prefs[nameKey(id)] ?: return null
        val mfcc    = parseMfcc(prefs[mfccKey(id)] ?: return null) ?: return null
        val samples = prefs[samplesKey(id)] ?: 1
        return VoiceProfile(id, name, mfcc, samples)
    }

    private fun parseMfcc(s: String): FloatArray? {
        val arr = s.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        return if (arr.isEmpty()) null else arr
    }
}
