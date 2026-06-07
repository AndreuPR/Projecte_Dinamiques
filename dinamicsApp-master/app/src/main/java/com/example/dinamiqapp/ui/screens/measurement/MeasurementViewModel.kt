package com.example.dinamiqapp.ui.screens.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.FilteredAudioSource
import com.example.dinamiqapp.audio.SignalConfig
import com.example.dinamiqapp.audio.SignalProcessor
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

    private val _keepLearning = MutableStateFlow(false)
    val keepLearning: StateFlow<Boolean> = _keepLearning

    val hasActiveVoice: Boolean get() = FilteredAudioSource.activeVoice != null

    private val INACTIVITY_STOP_MS = 60_000L
    private var lastVoiceActivityMs = System.currentTimeMillis()

    fun toggleKeepLearning() { _keepLearning.value = !_keepLearning.value }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMeasuring() {
        viewModelScope.launch {
            val refreshMs   = repository.appRefreshMs.first().toLong()
            val profileName = repository.activeProfileName.first()
            val rangesFlow  = repository.profileRanges(profileName)

            // Configura el motor de senyal
            processor.updateConfig(SignalConfig(
                emaAlpha        = repository.signalEmaAlpha.first(),
                hysteresisCount = repository.signalHysteresis.first()
            ))

            // Carrega el model de veu a FilteredAudioSource (porta d'entrada única)
            val voice   = voiceRepository.activeProfile()
            val voiceId = voiceRepository.activeVoiceId.first()
            FilteredAudioSource.setVoice(voice)
            FilteredAudioSource.similarityThreshold = repository.voiceSimilarityThreshold.first()

            // Keep-learning: actualitza el model quan arriben frames acceptats
            FilteredAudioSource.onFrameAccepted = { mfcc ->
                val now = System.currentTimeMillis()
                lastVoiceActivityMs = now
                if (_keepLearning.value && voiceId.isNotBlank()) {
                    viewModelScope.launch { voiceRepository.improveProfile(voiceId, mfcc) }
                }
            }

            // Comprova auto-stop per inactivitat (es gestiona al collect)
            FilteredAudioSource.filteredFrameFlow(refreshMs)
                .combine(rangesFlow) { frame, ranges ->
                    ScaleConverter.updateFromRanges(ranges)
                    val reading   = processor.process(frame, ranges)
                    val dbDisplay = processor.lastSmoothedDb ?: frame.db
                    val intensity = ScaleConverter.dbToScale(dbDisplay).roundToInt().coerceIn(0, 100)
                    Triple(intensity, reading.level, reading.precision)
                }.collect { (intensity, level, precision) ->
                    _soundIntensity.value = intensity
                    _currentLevel.value   = level
                    _precision.value      = precision

                    // Auto-stop aprenentatge si fa massa estona sense activitat
                    if (_keepLearning.value &&
                        System.currentTimeMillis() - lastVoiceActivityMs > INACTIVITY_STOP_MS) {
                        _keepLearning.value = false
                    }
                }
        }
    }
}
