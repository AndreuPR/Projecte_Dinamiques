package com.example.dinamiqapp.audio

import com.example.dinamiqapp.data.AudioReading
import com.example.dinamiqapp.data.DynamicLevel
import com.example.dinamiqapp.data.DynamicRange
import com.example.dinamiqapp.data.classifyDb

/**
 * Encapsula tot el processament del senyal d'àudio:
 * suavitzat EMA + histèresi de canvi de dinàmica.
 *
 * Quan s'implementi TensorFlow, es crearà una subclasse o implementació
 * alternativa d'aquesta interfície sense tocar res més.
 */
interface AudioEngine {
    fun process(db: Float, ranges: Map<DynamicLevel, DynamicRange>): AudioReading
    fun reset()
}

data class SignalConfig(
    val emaAlpha: Float = 0.25f,      // 0.05 (molt suau) – 1.0 (sense suavitzat)
    val hysteresisCount: Int = 3,      // lectures consecutives per confirmar canvi
)

class SignalProcessor(private var config: SignalConfig = SignalConfig()) : AudioEngine {

    private var smoothedDb: Float? = null
    private var candidateLevel: DynamicLevel? = null
    private var candidateCount: Int = 0
    private var confirmedLevel: DynamicLevel? = null

    fun updateConfig(newConfig: SignalConfig) {
        config = newConfig
        reset()
    }

    override fun reset() {
        smoothedDb = null
        candidateLevel = null
        candidateCount = 0
        confirmedLevel = null
    }

    override fun process(db: Float, ranges: Map<DynamicLevel, DynamicRange>): AudioReading {
        // EMA: suavitza el senyal brut
        smoothedDb = if (smoothedDb == null) db
                     else config.emaAlpha * db + (1f - config.emaAlpha) * smoothedDb!!

        val sDb = smoothedDb!!
        val reading = classifyDb(sDb, ranges)

        // Histèresi: acumula lectures de la mateixa dinàmica candidata
        if (reading.level == candidateLevel) {
            candidateCount++
        } else {
            candidateLevel = reading.level
            candidateCount = 1
        }

        // Canvia la dinàmica confirmada només quan la candidata es manté prou lectures
        if (candidateCount >= config.hysteresisCount) {
            confirmedLevel = candidateLevel
        }

        return reading.copy(level = confirmedLevel)
    }
}
