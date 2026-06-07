package com.example.dinamiqapp.audio

import com.example.dinamiqapp.data.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * PORTA D'ENTRADA ÚNICA per a tot l'àudio de l'app.
 *
 * Si hi ha un model de veu actiu, només deixa passar els frames
 * que coincideixen amb aquell instrument/veu.
 * Si no hi ha model, deixa passar tot (mode sense filtre).
 *
 * EXCEPCIÓ: l'entrenament de veu (VoiceManagementViewModel) usa
 * AudioMeter directament, ja que necessita àudio brut per construir el model.
 */
object FilteredAudioSource {

    var activeVoice: VoiceProfile? = null
    var similarityThreshold: Float = 0.82f

    // Callback per al "seguir aprenent" — crida quan un frame és acceptat
    var onFrameAccepted: ((mfcc: FloatArray) -> Unit)? = null

    /**
     * Flow de frames filtrats pel model de veu actiu.
     * Tots els consumers de l'app han d'usar aquest flow.
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun filteredFrameFlow(windowMs: Long = 200): Flow<AudioFrame> =
        AudioMeter.audioFrameFlow(windowMs)
            .filter { frame -> accepts(frame.pcm) }

    /**
     * Versió simplificada que només emet el dB (per al calibratge).
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun filteredDbFlow(windowMs: Long = 200): Flow<Float> =
        filteredFrameFlow(windowMs).map { it.db }

    fun setVoice(profile: VoiceProfile?) {
        activeVoice = profile
    }

    fun clearVoice() {
        activeVoice = null
    }

    // ── Privat ──────────────────────────────────────────────────────────────

    private fun accepts(pcm: ShortArray): Boolean {
        val voice = activeVoice ?: return true   // sense model = tot passa
        val mfcc  = MfccExtractor.extract(pcm)
        val sim   = MfccExtractor.cosineSimilarity(mfcc, voice.mfccCentroid)
        return if (sim >= similarityThreshold) {
            onFrameAccepted?.invoke(mfcc)
            true
        } else {
            false
        }
    }
}
