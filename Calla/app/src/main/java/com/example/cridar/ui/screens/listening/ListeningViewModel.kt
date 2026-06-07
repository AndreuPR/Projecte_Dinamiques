package com.example.cridar.ui.screens.listening


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cridar.audio.AudioMeter
import com.example.cridar.audio.ScaleConverter
import com.example.cridar.audio.TextToSpeechManager
import com.example.cridar.data.ShoutRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ListeningViewModel(application: Application,) : AndroidViewModel(application) {
    private val repository = ShoutRepository(application)

    private val _isShouting = MutableStateFlow(false)
    val isShouting: StateFlow<Boolean> = _isShouting

    private val _voiceEnabled = MutableStateFlow(false)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled

    init {
        viewModelScope.launch {
            repository.voiceEnabled.collect { _voiceEnabled.value = it }
        }
    }

    private var listeningJob: Job? = null
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (listeningJob?.isActive == true) return
        listeningJob = viewModelScope.launch  {
            val refreshMs = repository.refreshMs.first().toLong()
            val threshold = repository.threshold.first()
            AudioMeter.dbFlow(refreshMs).collect { db ->
                val intensity = ScaleConverter.dbToScale(db)
                if (intensity >= threshold) {
                    _isShouting.value = true
                    cancel()
                }
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
    }


}