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

    // Toggle "seguir aprenent" controlable des de la pantalla de mesura
    private val _keepLearning = MutableStateFlow(false)
    val keepLearning: StateFlow<Boolean> = _keepLearning

    val hasActiveVoice: Boolean get() = processor.activeVoice != null

    // Temps en ms sense activitat de veu reconeguda → atura l'aprenentatge
    private val INACTIVITY_STOP_MS = 60_000L
    private var lastVoiceActivityMs = 0L

    fun toggleKeepLearning() { _keepLearning.value = !_keepLearning.value }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMeasuring() {
        viewModelScope.launch {
            val refreshMs   = repository.appRefreshMs.first().toLong()
            val profileName = repository.activeProfileName.first()
            val rangesFlow  = repository.profileRanges(profileName)

            processor.updateConfig(SignalConfig(
                emaAlpha        = repository.signalEmaAlpha.first(),
                hysteresisCount = repository.signalHysteresis.first()
            ))

            processor.activeVoice = voiceRepository.activeProfile()
            val voiceId = voiceRepository.activeVoiceId.first()

            // Callback per al keep-learning amb auto-stop per inactivitat
            if (processor.activeVoice != null) {
                processor.onSimilarityMeasured = { mfcc, sim ->
                    if (sim >= 0.90f) {
                        lastVoiceActivityMs = System.currentTimeMillis()
                        val inactive = System.currentTimeMillis() - lastVoiceActivityMs > INACTIVITY_STOP_MS
                        if (_keepLearning.value && !inactive && voiceId.isNotBlank()) {
                            viewModelScope.launch { voiceRepository.improveProfile(voiceId, mfcc) }
                        }
                    } else {
                        // Si fa massa estona sense reconèixer la veu, atura l'aprenentatge
                        if (System.currentTimeMillis() - lastVoiceActivityMs > INACTIVITY_STOP_MS) {
                            _keepLearning.value = false
                        }
                    }
                }
            }

            AudioMeter.audioFrameFlow(refreshMs).combine(rangesFlow) { frame, ranges ->
                ScaleConverter.updateFromRanges(ranges)
                val reading = processor.process(frame, ranges)

                // Intensitat: usa el dB suavitzat del processor (la mateixa font que la dinàmica)
                val dbForDisplay = processor.lastSmoothedDb ?: frame.db
                val intensity = ScaleConverter.dbToScale(dbForDisplay).roundToInt().coerceIn(0, 100)

                Triple(intensity, reading.level, reading.precision)
            }.collect { (intensity, level, precision) ->
                _soundIntensity.value = intensity
                _currentLevel.value   = level
                _precision.value      = precision
            }
        }
    }
}
