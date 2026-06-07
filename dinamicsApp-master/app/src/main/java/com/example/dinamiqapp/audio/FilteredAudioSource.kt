package com.example.dinamiqapp.audio

import android.content.Context
import com.example.dinamiqapp.audio.engines.EngineType
import com.example.dinamiqapp.audio.engines.MfccVoiceEngine
import com.example.dinamiqapp.audio.engines.TensorFlowVoiceEngine
import com.example.dinamiqapp.data.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * PORTA D'ENTRADA ÚNICA per a tot l'àudio de l'app.
 *
 * Si hi ha un model de veu actiu, només deixa passar els frames
 * que coincideixen amb aquell instrument/veu, usant el motor configurat:
 *   - EngineType.MFCC        → MfccVoiceEngine (template matching, 13 dims)
 *   - EngineType.TENSORFLOW  → TensorFlowVoiceEngine (YAMNet, 1024 dims)
 *
 * Si no hi ha model, deixa passar tot (mode sense filtre).
 *
 * EXCEPCIÓ: l'entrenament de veu (VoiceManagementViewModel) usa
 * AudioMeter directament, ja que necessita àudio brut per construir el model.
 *
 * INICIALITZACIÓ: cridar initialize(context) a MainActivity.onCreate()
 * perquè el motor TF Lite pugui carregar l'asset yamnet.tflite.
 */
object FilteredAudioSource {

    var activeVoice: VoiceProfile? = null
    var similarityThreshold: Float = 0.82f

    // Callback per al "seguir aprenent" — crida quan un frame és acceptat
    var onFrameAccepted: ((features: FloatArray) -> Unit)? = null

    // Motor TF Lite (s'inicialitza lazy en cridar initialize())
    private var _tfEngine: TensorFlowVoiceEngine? = null
    val tfEngine: TensorFlowVoiceEngine? get() = _tfEngine

    /** Cal cridar-lo a MainActivity.onCreate() per carregar el model YAMNet */
    fun initialize(context: Context) {
        if (_tfEngine == null) {
            _tfEngine = TensorFlowVoiceEngine(context.applicationContext)
        }
    }

    /** True si el model YAMNet està disponible a assets/ */
    val isTfModelAvailable: Boolean get() = _tfEngine?.isAvailable ?: false

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
        // Reset buffer del motor TF quan es canvia de veu
        if (profile?.engineType == EngineType.TENSORFLOW) {
            _tfEngine?.reset()
        }
    }

    fun clearVoice() {
        activeVoice = null
    }

    // ── Privat ──────────────────────────────────────────────────────────────

    private fun accepts(pcm: ShortArray): Boolean {
        val voice = activeVoice ?: return true   // sense model = tot passa

        return when (voice.engineType) {

            EngineType.TENSORFLOW -> {
                val engine = _tfEngine ?: return acceptsMfcc(pcm, voice) // fallback si no hi ha TF
                val embedding = engine.extract(pcm)
                if (embedding.all { it == 0f }) return false  // buffer d'escalfament (~1s inicial)
                val sim = engine.similarity(embedding, voice.mfccCentroid)
                if (sim >= similarityThreshold) { onFrameAccepted?.invoke(embedding); true }
                else false
            }

            EngineType.MFCC -> acceptsMfcc(pcm, voice)
        }
    }

    private fun acceptsMfcc(pcm: ShortArray, voice: VoiceProfile): Boolean {
        val mfcc   = MfccVoiceEngine.extract(pcm)
        // Comprova tots els centroids (greu, mig, agut...): n'hi ha prou amb un que encaixi
        val maxSim = voice.allCentroids.maxOf { MfccVoiceEngine.similarity(mfcc, it) }
        return if (maxSim >= similarityThreshold) { onFrameAccepted?.invoke(mfcc); true }
               else false
    }
}
