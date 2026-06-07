package com.example.dinamiqapp.audio

import com.example.dinamiqapp.data.AudioReading
import com.example.dinamiqapp.data.DynamicLevel
import com.example.dinamiqapp.data.DynamicRange
import com.example.dinamiqapp.data.classifyDb

/**
 * Motor de processament de senyal.
 * Rep àudio JA FILTRAT per FilteredAudioSource i aplica:
 *   1. Suavitzat EMA
 *   2. Histèresi (evita canvis sobtats de dinàmica)
 *   3. Classificació en dinàmica musical
 *
 * NO fa filtre de veu — això és responsabilitat de FilteredAudioSource.
 */
interface AudioEngine {
    fun process(frame: AudioFrame, ranges: Map<DynamicLevel, DynamicRange>): AudioReading
    fun reset()
}

data class SignalConfig(
    val emaAlpha: Float = 0.25f,
    val hysteresisCount: Int = 3
)

class SignalProcessor(private var config: SignalConfig = SignalConfig()) : AudioEngine {

    private var smoothedDb: Float? = null
    private var candidateLevel: DynamicLevel? = null
    private var candidateCount: Int = 0
    private var confirmedLevel: DynamicLevel? = null

    // dB suavitzat exposat per a la UI (coherent amb la classificació)
    var lastSmoothedDb: Float? = null
        private set

    fun updateConfig(newConfig: SignalConfig) {
        config = newConfig
        reset()
    }

    override fun reset() {
        smoothedDb     = null
        lastSmoothedDb = null
        candidateLevel = null
        candidateCount = 0
        confirmedLevel = null
    }

    override fun process(frame: AudioFrame, ranges: Map<DynamicLevel, DynamicRange>): AudioReading {
        // 1. EMA: suavitza el senyal
        smoothedDb = if (smoothedDb == null) frame.db
                     else config.emaAlpha * frame.db + (1f - config.emaAlpha) * smoothedDb!!
        val sDb = smoothedDb!!
        lastSmoothedDb = sDb

        // 2. Classifica
        val reading = classifyDb(sDb, ranges)

        // 3. Histèresi: acumula lectures de la mateixa dinàmica candidata
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
}
