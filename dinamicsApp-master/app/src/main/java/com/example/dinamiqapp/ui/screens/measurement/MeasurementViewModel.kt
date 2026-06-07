package com.example.dinamiqapp.ui.screens.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.SignalConfig
import com.example.dinamiqapp.audio.SignalProcessor
import com.example.dinamiqapp.audio.AudioMeter
import com.example.dinamiqapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val processor = SignalProcessor()

    private val _soundIntensity = MutableStateFlow(0)
    val soundIntensity: StateFlow<Int> = _soundIntensity

    private val _currentLevel = MutableStateFlow<DynamicLevel?>(null)
    val currentLevel: StateFlow<DynamicLevel?> = _currentLevel

    private val _precision = MutableStateFlow(0f)
    val precision: StateFlow<Float> = _precision

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMeasuring() {
        viewModelScope.launch {
            val refreshMs = repository.appRefreshMs.first().toLong()
            val profileName = repository.activeProfileName.first()
            val rangesFlow = repository.profileRanges(profileName)

            // Carrega config del motor des del DataStore
            val config = SignalConfig(
                emaAlpha = repository.signalEmaAlpha.first(),
                hysteresisCount = repository.signalHysteresis.first()
            )
            processor.updateConfig(config)

            AudioMeter.dbFlow(refreshMs).combine(rangesFlow) { db, ranges ->
                ScaleConverter.updateFromRanges(ranges)
                val reading = processor.process(db, ranges)
                val intensity = ScaleConverter.dbToScale(db).roundToInt().coerceIn(0, 100)
                Triple(intensity, reading.level, reading.precision)
            }.collect { (intensity, level, precision) ->
                _soundIntensity.value = intensity
                _currentLevel.value = level
                _precision.value = precision
            }
        }
    }
}
