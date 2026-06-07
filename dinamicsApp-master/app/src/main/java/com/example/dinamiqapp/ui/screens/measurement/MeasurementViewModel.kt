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
    private val repository      = SettingsRepository(application)
    private val voiceRepository = VoiceRepository(application)
    private val processor       = SignalProcessor()

    private val _soundIntensity = MutableStateFlow(0)
    val soundIntensity: StateFlow<Int> = _soundIntensity

    private val _currentLevel = MutableStateFlow<DynamicLevel?>(null)
    val currentLevel: StateFlow<DynamicLevel?> = _currentLevel

    private val _precision = MutableStateFlow(0f)
    val precision: StateFlow<Float> = _precision

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMeasuring() {
        viewModelScope.launch {
            val refreshMs   = repository.appRefreshMs.first().toLong()
            val profileName = repository.activeProfileName.first()
            val rangesFlow  = repository.profileRanges(profileName)

            // Carrega config del motor
            processor.updateConfig(SignalConfig(
                emaAlpha         = repository.signalEmaAlpha.first(),
                hysteresisCount  = repository.signalHysteresis.first()
            ))

            // Carrega perfil de veu actiu (pot ser null)
            processor.activeVoice = voiceRepository.activeProfile()

            // "Seguir aprenent": si està actiu, millorem el model en segon pla
            val keepLearning = voiceRepository.keepLearning.first()
            if (keepLearning && processor.activeVoice != null) {
                val voiceId = voiceRepository.activeVoiceId.first()
                processor.onSimilarityMeasured = { mfcc, sim ->
                    // Actualitzem el model únicament quan estem segurs que és la veu correcta
                    if (sim >= 0.90f) {
                        viewModelScope.launch { voiceRepository.improveProfile(voiceId, mfcc) }
                    }
                }
            }

            AudioMeter.audioFrameFlow(refreshMs).combine(rangesFlow) { frame, ranges ->
                ScaleConverter.updateFromRanges(ranges)
                val reading   = processor.process(frame, ranges)
                val intensity = ScaleConverter.dbToScale(frame.db).roundToInt().coerceIn(0, 100)
                Triple(intensity, reading.level, reading.precision)
            }.collect { (intensity, level, precision) ->
                _soundIntensity.value = intensity
                _currentLevel.value   = level
                _precision.value      = precision
            }
        }
    }
}
