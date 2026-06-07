package com.example.dinamiqapp.audio

import com.example.dinamiqapp.data.AudioReading
import com.example.dinamiqapp.data.DynamicLevel
import com.example.dinamiqapp.data.DynamicRange
import com.example.dinamiqapp.data.VoiceProfile
import com.example.dinamiqapp.data.classifyDb

interface AudioEngine {
    fun process(frame: AudioFrame, ranges: Map<DynamicLevel, DynamicRange>): AudioReading
    fun reset()
}

data class SignalConfig(
    val emaAlpha: Float = 0.25f,
    val hysteresisCount: Int = 3,
    val voiceSimilarityThreshold: Float = 0.82f
)

class SignalProcessor(private var config: SignalConfig = SignalConfig()) : AudioEngine {

    private var smoothedDb: Float? = null
    private var candidateLevel: DynamicLevel? = null
    private var candidateCount: Int = 0
    private var confirmedLevel: DynamicLevel? = null

    // Exposem el dB suavitzat per a la UI (sempre coherent amb la classificació)
    var lastSmoothedDb: Float? = null
        private set

    // Última similitud de veu calculada (0..1), exposada per a debug/UI
    var lastVoiceSimilarity: Float = 0f
        private set

    // Perfil de veu actiu (null = sense filtre)
    var activeVoice: VoiceProfile? = null

    // Callback per notificar similitud calculada (útil per al "keep learning")
    var onSimilarityMeasured: ((mfcc: FloatArray, similarity: Float) -> Unit)? = null

    fun updateConfig(newConfig: SignalConfig) {
        config = newConfig
        reset()
    }

    override fun reset() {
        smoothedDb = null
        lastSmoothedDb = null
        lastVoiceSimilarity = 0f
        candidateLevel = null
        candidateCount = 0
        confirmedLevel = null
    }

    override fun process(frame: AudioFrame, ranges: Map<DynamicLevel, DynamicRange>): AudioReading {
        // 1. Filtre de veu (si hi ha perfil actiu)
        val voiceAccepted = checkVoice(frame.pcm)
        if (!voiceAccepted) {
            // Frame rebutjat: no actualitzem res, retornem l'últim estat conegut
            // lastSmoothedDb queda igual → la UI mostra l'últim valor vàlid
            return AudioReading(lastSmoothedDb ?: frame.db, confirmedLevel, 0f)
        }

        // 2. EMA: suavitza el senyal (font única per al número I per a la classificació)
        smoothedDb = if (smoothedDb == null) frame.db
                     else config.emaAlpha * frame.db + (1f - config.emaAlpha) * smoothedDb!!

        val sDb = smoothedDb!!
        lastSmoothedDb = sDb   // exposem per a la UI

        val reading = classifyDb(sDb, ranges)

        // 3. Histèresi
        if (reading.level == candidateLevel) {
            candidateCount++
        } else {
            candidateLevel = reading.level
            candidateCount = 1
        }
        if (candidateCount >= config.hysteresisCount) {
            confirmedLevel = candidateLevel
        }

        return reading.copy(level = confirmedLevel)
    }

    /**
     * Comprova si el frame PCM coincideix amb la veu activa.
     * Retorna true si no hi ha perfil actiu (sense filtre).
     */
    private fun checkVoice(pcm: ShortArray): Boolean {
        val voice = activeVoice ?: return true
        val mfcc  = MfccExtractor.extract(pcm)
        val sim   = MfccExtractor.cosineSimilarity(mfcc, voice.mfccCentroid)
        lastVoiceSimilarity = sim
        onSimilarityMeasured?.invoke(mfcc, sim)
        return sim >= config.voiceSimilarityThreshold
    }
}
