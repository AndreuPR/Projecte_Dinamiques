package com.example.dinamiqapp.ui.screens.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.AudioMeter
import com.example.dinamiqapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _soundIntensity = MutableStateFlow(0)
    val soundIntensity: StateFlow<Int> = _soundIntensity

    private val _currentLevel = MutableStateFlow<DynamicLevel?>(null)
    val currentLevel: StateFlow<DynamicLevel?> = _currentLevel

    private val _precision = MutableStateFlow(0f)
    val precision: StateFlow<Float> = _precision

    companion object {
        // Suavitzat EMA: 0.0 = màxim suavitzat, 1.0 = sense suavitzat
        // 0.25 = canvis suaus però responsiu
        private const val EMA_ALPHA = 0.25f

        // Histèresi: cal que la nova dinàmica es mantingui N lectures consecutives
        // abans de canviar la que es mostra. Amb 200ms/lectura, 3 = 600ms de debounce
        private const val HYSTERESIS_COUNT = 3
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMeasuring() {
        viewModelScope.launch {
            val refreshMs = repository.appRefreshMs.first().toLong()
            val profileName = repository.activeProfileName.first()
            val rangesFlow = repository.profileRanges(profileName)

            var smoothedDb: Float? = null
            var candidateLevel: DynamicLevel? = null
            var candidateCount = 0

            AudioMeter.dbFlow(refreshMs).combine(rangesFlow) { db, ranges ->
                ScaleConverter.updateFromRanges(ranges)

                // EMA: suavitza el senyal d'àudio brut
                smoothedDb = if (smoothedDb == null) db
                             else EMA_ALPHA * db + (1f - EMA_ALPHA) * smoothedDb!!

                val sDb = smoothedDb!!
                val intensity = ScaleConverter.dbToScale(sDb).roundToInt().coerceIn(0, 100)
                val reading = classifyDb(sDb, ranges)

                // Histèresi: acumula lectures de la mateixa dinàmica candidata
                if (reading.level == candidateLevel) {
                    candidateCount++
                } else {
                    candidateLevel = reading.level
                    candidateCount = 1
                }

                // Només canviem la dinàmica mostrada si la candidata es confirma
                val confirmedLevel = if (candidateCount >= HYSTERESIS_COUNT) candidateLevel
                                     else _currentLevel.value

                Triple(intensity, confirmedLevel, reading.precision)
            }.collect { (intensity, level, precision) ->
                _soundIntensity.value = intensity
                _currentLevel.value = level
                _precision.value = precision
            }
        }
    }
}
