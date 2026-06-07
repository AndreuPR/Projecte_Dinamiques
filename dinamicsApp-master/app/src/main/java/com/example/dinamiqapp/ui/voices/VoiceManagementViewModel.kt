package com.example.dinamiqapp.ui.voices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.AudioMeter
import com.example.dinamiqapp.audio.MfccExtractor
import com.example.dinamiqapp.data.VoiceProfile
import com.example.dinamiqapp.data.VoiceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class TrainState {
    object Idle : TrainState()
    data class Countdown(val secondsLeft: Int) : TrainState()
    data class Recording(val secondsLeft: Int) : TrainState()
    data class Done(val mfcc: FloatArray) : TrainState()
}

class VoiceManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceRepository(application)

    val profiles: StateFlow<List<VoiceProfile>> =
        repository.allProfiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val activeVoiceId: StateFlow<String> =
        repository.activeVoiceId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val _trainState = MutableStateFlow<TrainState>(TrainState.Idle)
    val trainState: StateFlow<TrainState> = _trainState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var trainJob: Job? = null

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startTraining(durationSeconds: Int = 10) {
        if (trainJob?.isActive == true) return
        _message.value = null

        trainJob = viewModelScope.launch {
            try {
                // Compte enrere
                for (s in 3 downTo 1) {
                    _trainState.value = TrainState.Countdown(s)
                    delay(1000L)
                }

                // Gravació: acumula frames PCM per calcular centroid MFCC
                val allMfcc = mutableListOf<FloatArray>()
                val startMs = System.currentTimeMillis()
                val durationMs = durationSeconds * 1000L

                AudioMeter.audioFrameFlow(100L)
                    .takeWhile { System.currentTimeMillis() - startMs < durationMs }
                    .collect { frame ->
                        val secondsLeft = ((durationMs - (System.currentTimeMillis() - startMs)) / 1000L).toInt() + 1
                        _trainState.value = TrainState.Recording(secondsLeft)
                        allMfcc.add(MfccExtractor.extract(frame.pcm))
                    }

                if (allMfcc.size < 5) {
                    _message.value = "Massa poc àudio. Intenta-ho de nou."
                    _trainState.value = TrainState.Idle
                    return@launch
                }

                // Centroid: mitjana de tots els vectors MFCC
                val centroid = FloatArray(MfccExtractor.NUM_COEFFICIENTS) { i ->
                    allMfcc.sumOf { it[i].toDouble() }.toFloat() / allMfcc.size
                }
                _trainState.value = TrainState.Done(centroid)

            } catch (e: Exception) {
                _message.value = "Error durant l'entrenament."
                _trainState.value = TrainState.Idle
            }
        }
    }

    fun saveNewVoice(name: String, mfcc: FloatArray) {
        viewModelScope.launch {
            val profile = VoiceProfile(
                id           = repository.newId(),
                name         = name.ifBlank { "Veu sense nom" },
                mfccCentroid = mfcc,
                sampleCount  = 1
            )
            repository.saveProfile(profile)
            _message.value = "Veu '${profile.name}' guardada!"
            _trainState.value = TrainState.Idle
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun improveVoice(id: String, durationSeconds: Int = 10) {
        if (trainJob?.isActive == true) return
        _message.value = null

        trainJob = viewModelScope.launch {
            try {
                for (s in 3 downTo 1) {
                    _trainState.value = TrainState.Countdown(s)
                    delay(1000L)
                }
                val allMfcc = mutableListOf<FloatArray>()
                val startMs = System.currentTimeMillis()
                val durationMs = durationSeconds * 1000L

                AudioMeter.audioFrameFlow(100L)
                    .takeWhile { System.currentTimeMillis() - startMs < durationMs }
                    .collect { frame ->
                        val secondsLeft = ((durationMs - (System.currentTimeMillis() - startMs)) / 1000L).toInt() + 1
                        _trainState.value = TrainState.Recording(secondsLeft)
                        allMfcc.add(MfccExtractor.extract(frame.pcm))
                    }

                if (allMfcc.size < 5) {
                    _message.value = "Massa poc àudio."
                    _trainState.value = TrainState.Idle
                    return@launch
                }

                val newCentroid = FloatArray(MfccExtractor.NUM_COEFFICIENTS) { i ->
                    allMfcc.sumOf { it[i].toDouble() }.toFloat() / allMfcc.size
                }
                repository.improveProfile(id, newCentroid)
                _message.value = "Model millorat!"
                _trainState.value = TrainState.Idle

            } catch (e: Exception) {
                _message.value = "Error durant l'entrenament."
                _trainState.value = TrainState.Idle
            }
        }
    }

    fun setActiveVoice(id: String) {
        viewModelScope.launch { repository.setActiveVoice(id) }
    }

    fun deleteVoice(id: String) {
        viewModelScope.launch {
            repository.deleteProfile(id)
            _message.value = "Veu eliminada."
        }
    }

    fun cancelTraining() {
        trainJob?.cancel()
        _trainState.value = TrainState.Idle
    }

    fun clearMessage() { _message.value = null }
}
