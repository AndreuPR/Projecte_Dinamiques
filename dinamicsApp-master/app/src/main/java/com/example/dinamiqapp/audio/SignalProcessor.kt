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
    val hysteresisCount: Int = 3,
    /**
     * Ràtio atac/alliberament asimètric.
     * > 1.0 = l'atac (pujada) és més ràpid que l'alliberament (baixada).
     * Exemple: 3.0 → quan el so puja, l'alpha efectiu és emaAlpha × 3 (fins a 1.0).
     * Comportament natural de VU meter: reacciona ràpid als crescendos,
     * baixa suaument en els diminuendos i silencis breus.
     */
    val attackReleaseRatio: Float = 3.0f
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
        // 1. EMA asimètrica: atac ràpid, alliberament lent
        //    Quan el so puja (crescendo) reaccionem de pressa.
        //    Quan baixa (diminuendo o silenci breu) baixem suaument.
        val prev = smoothedDb
        val alpha = if (prev == null || frame.db > prev) {
            (config.emaAlpha * config.attackReleaseRatio).coerceAtMost(1.0f)  // atac
        } else {
            config.emaAlpha                                                     // alliberament
        }
        smoothedDb = if (prev == null) frame.db else alpha * frame.db + (1f - alpha) * prev
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
