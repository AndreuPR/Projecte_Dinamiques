package com.example.dinamiqapp.ui.screens.appsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.FilteredAudioSource
import com.example.dinamiqapp.data.ScaleConverter
import com.example.dinamiqapp.data.SettingsRepository
import com.example.dinamiqapp.data.VoiceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository      = SettingsRepository(application)
    private val voiceRepository = VoiceRepository(application)

    // Àudio global
    private val _minDb = MutableStateFlow(-70f)
    private val _maxDb = MutableStateFlow(0f)
    private val _refreshMs = MutableStateFlow(200)

    // Motor de senyal
    private val _emaAlpha = MutableStateFlow(0.25f)
    private val _hysteresis = MutableStateFlow(3)
    private val _ppPercentile = MutableStateFlow(0.15f)
    private val _ffPercentile  = MutableStateFlow(0.90f)
    private val _keepLearning        = MutableStateFlow(false)
    private val _similarityThreshold = MutableStateFlow(0.82f)
    private val _attackRelease       = MutableStateFlow(3.0f)

    val minDb: StateFlow<Float> = _minDb
    val maxDb: StateFlow<Float> = _maxDb
    val refreshMs: StateFlow<Int> = _refreshMs
    val emaAlpha: StateFlow<Float> = _emaAlpha
    val hysteresis: StateFlow<Int> = _hysteresis
    val ppPercentile: StateFlow<Float> = _ppPercentile
    val ffPercentile: StateFlow<Float>  = _ffPercentile
    val keepLearning: StateFlow<Boolean> = _keepLearning
    val similarityThreshold: StateFlow<Float> = _similarityThreshold
    val attackRelease: StateFlow<Float> = _attackRelease

    init {
        viewModelScope.launch {
            repository.appMinDb.first().let { _minDb.value = it }
            repository.appMaxDb.first().let { _maxDb.value = it }
            repository.appRefreshMs.first().let { _refreshMs.value = it }
            repository.signalEmaAlpha.first().let { _emaAlpha.value = it }
            repository.signalHysteresis.first().let { _hysteresis.value = it }
            repository.signalPpPercentile.first().let { _ppPercentile.value = it }
            repository.signalFfPercentile.first().let { _ffPercentile.value = it }
            voiceRepository.keepLearning.first().let { _keepLearning.value = it }
            repository.voiceSimilarityThreshold.first().let { _similarityThreshold.value = it }
            repository.signalAttackRelease.first().let { _attackRelease.value = it }
        }
    }

    fun updateMinDb(value: Float) { _minDb.value = value }
    fun updateMaxDb(value: Float) { _maxDb.value = value }
    fun updateRefreshMs(value: Int) { _refreshMs.value = value }
    fun updateEmaAlpha(value: Float) { _emaAlpha.value = value }
    fun updateHysteresis(value: Int) { _hysteresis.value = value }
    fun updatePpPercentile(value: Float) { _ppPercentile.value = value }
    fun updateFfPercentile(value: Float) { _ffPercentile.value = value }
    fun updateKeepLearning(value: Boolean) { _keepLearning.value = value }
    fun updateSimilarityThreshold(value: Float) { _similarityThreshold.value = value }
    fun updateAttackRelease(value: Float) { _attackRelease.value = value }

    fun save() {
        viewModelScope.launch {
            val min = _minDb.value.coerceIn(-90f, _maxDb.value - 1f)
            val max = _maxDb.value.coerceIn(min + 1f, 0f)
            val refresh = _refreshMs.value.coerceIn(50, 1000)
            repository.setAppMinDb(min)
            repository.setAppMaxDb(max)
            repository.setAppRefreshMs(refresh)
            ScaleConverter.appMinDb = min
            ScaleConverter.appMaxDb = max

            repository.setSignalEmaAlpha(_emaAlpha.value.coerceIn(0.05f, 1.0f))
            repository.setSignalHysteresis(_hysteresis.value.coerceIn(1, 10))
            repository.setSignalPpPercentile(_ppPercentile.value.coerceIn(0.05f, 0.40f))
            repository.setSignalFfPercentile(_ffPercentile.value.coerceIn(0.60f, 0.99f))
            voiceRepository.setKeepLearning(_keepLearning.value)
            val sim = _similarityThreshold.value.coerceIn(0.50f, 0.99f)
            repository.setVoiceSimilarityThreshold(sim)
            FilteredAudioSource.similarityThreshold = sim
            repository.setSignalAttackRelease(_attackRelease.value.coerceIn(1.0f, 8.0f))
        }
    }
}
