package com.example.cridar.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cridar.audio.ScaleConverter
import com.example.cridar.data.ShoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ShoutRepository(application)

    private val _minDb = MutableStateFlow(-70f)
    private val _maxDb = MutableStateFlow(0f)
    private val _refreshMs = MutableStateFlow(200)
    private val _threshold = MutableStateFlow(75f)
    private val _voiceEnabled = MutableStateFlow(false)

    val minDb: StateFlow<Float> = _minDb
    val maxDb: StateFlow<Float> = _maxDb
    val refreshMs: StateFlow<Int> = _refreshMs
    val threshold: StateFlow<Float> = _threshold
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled

    init {
        viewModelScope.launch {
            _minDb.value = repository.minDb.first()
            _maxDb.value = repository.maxDb.first()
            _refreshMs.value = repository.refreshMs.first()
            _threshold.value = repository.threshold.first()
            _voiceEnabled.value = repository.voiceEnabled.first()
        }
    }

    fun updateMinDb(v: Float) { _minDb.value = v }
    fun updateMaxDb(v: Float) { _maxDb.value = v }
    fun updateRefreshMs(v: Int) { _refreshMs.value = v }
    fun updateThreshold(v: Float) { _threshold.value = v }
    fun updateVoiceEnabled(v: Boolean) { _voiceEnabled.value = v }

    fun save() {
        viewModelScope.launch {
            repository.setMinDb(_minDb.value)
            repository.setMaxDb(_maxDb.value)
            repository.setRefreshMs(_refreshMs.value)
            repository.setThreshold(_threshold.value)
            repository.setVoiceEnabled(_voiceEnabled.value)
            ScaleConverter.minDb = _minDb.value
            ScaleConverter.maxDb = _maxDb.value
        }
    }
}