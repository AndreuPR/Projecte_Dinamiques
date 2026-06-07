package com.example.dinamiqapp.ui.voices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.AudioMeter
import com.example.dinamiqapp.audio.FilteredAudioSource
import com.example.dinamiqapp.audio.MfccExtractor
import com.example.dinamiqapp.audio.engines.EngineType
import com.example.dinamiqapp.audio.engines.MfccVoiceEngine
import com.example.dinamiqapp.data.VoiceProfile
import com.example.dinamiqapp.data.VoiceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class TrainState {
    object Idle : TrainState()
    data class Countdown(val secondsLeft: Int, val phase: String = "") : TrainState()
    data class Recording(val secondsLeft: Int, val phase: String = "") : TrainState()
    // Entrenament simple (MFCC o TF Lite d'una sola fase)
    data class Done(val features: FloatArray, val engineType: EngineType = EngineType.MFCC) : TrainState()
    // Entrenament per registres (3 fases greu/mig/agut — només MFCC)
    data class MultiDone(val centroids: List<FloatArray>) : TrainState()
}

class VoiceManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VoiceRepository(application)

    // Accés al motor TF (ja inicialitzat a MainActivity)
    val isTfModelAvailable: Boolean get() = FilteredAudioSource.isTfModelAvailable

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
    fun startTraining(engineType: EngineType = EngineType.MFCC, durationSeconds: Int = 10) {
        if (trainJob?.isActive == true) return
        _message.value = null

        // Reinicia el buffer TF si cal
        if (engineType == EngineType.TENSORFLOW) FilteredAudioSource.tfEngine?.reset()

        trainJob = viewModelScope.launch {
            try {
                val engineLabel = if (engineType == EngineType.TENSORFLOW) "TF Lite" else ""

                // Compte enrere
                for (s in 3 downTo 1) {
                    _trainState.value = TrainState.Countdown(s, engineLabel)
                    delay(1000L)
                }

                // Gravació: acumula features (MFCC 13-dim o embeddings YAMNet 1024-dim)
                val allFeatures = mutableListOf<FloatArray>()
                val startMs    = System.currentTimeMillis()
                val durationMs = durationSeconds * 1000L
                val tfEngine   = FilteredAudioSource.tfEngine

                AudioMeter.audioFrameFlow(100L)
                    .takeWhile { System.currentTimeMillis() - startMs < durationMs }
                    .collect { frame ->
                        val secondsLeft = ((durationMs - (System.currentTimeMillis() - startMs)) / 1000L).toInt() + 1
                        _trainState.value = TrainState.Recording(secondsLeft, engineLabel)
                        val features = when (engineType) {
                            EngineType.TENSORFLOW -> tfEngine?.extract(frame.pcm) ?: MfccVoiceEngine.extract(frame.pcm)
                            EngineType.MFCC       -> MfccVoiceEngine.extract(frame.pcm)
                        }
                        // Ignora frames de l'escalfament del buffer TF (tot zeros)
                        if (features.any { it != 0f }) allFeatures.add(features)
                    }

                if (allFeatures.size < 5) {
                    _message.value = "Massa poc àudio. Intenta-ho de nou."
                    _trainState.value = TrainState.Idle
                    return@launch
                }

                // Centroid: mitjana de tots els vectors de característiques
                val dim      = allFeatures[0].size
                val centroid = FloatArray(dim) { i ->
                    allFeatures.sumOf { it[i].toDouble() }.toFloat() / allFeatures.size
                }
                _trainState.value = TrainState.Done(centroid, engineType)

            } catch (e: Exception) {
                _message.value = "Error durant l'entrenament."
                _trainState.value = TrainState.Idle
            }
        }
    }

    /**
     * Entrenament per registres: greu → mig → agut.
     * Genera 3 centroids que cobren tot el rang de l'instrument.
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startMultiRegisterTraining(durationSecondsPerPhase: Int = 8) {
        if (trainJob?.isActive == true) return
        _message.value = null

        val phases = listOf(
            "Registre greu"  to "Toca les notes més greus",
            "Registre mig"   to "Toca notes del mig",
            "Registre agut"  to "Toca les notes més agudes"
        )

        trainJob = viewModelScope.launch {
            try {
                val centroids = mutableListOf<FloatArray>()

                for ((phaseName, _) in phases) {
                    // Compte enrere per a cada fase
                    for (s in 3 downTo 1) {
                        _trainState.value = TrainState.Countdown(s, phaseName)
                        delay(1000L)
                    }

                    // Gravació de la fase
                    val allMfcc = mutableListOf<FloatArray>()
                    val startMs = System.currentTimeMillis()
                    val durationMs = durationSecondsPerPhase * 1000L

                    AudioMeter.audioFrameFlow(100L)
                        .takeWhile { System.currentTimeMillis() - startMs < durationMs }
                        .collect { frame ->
                            val secondsLeft = ((durationMs - (System.currentTimeMillis() - startMs)) / 1000L).toInt() + 1
                            _trainState.value = TrainState.Recording(secondsLeft, phaseName)
                            allMfcc.add(MfccExtractor.extract(frame.pcm))
                        }

                    if (allMfcc.size < 5) {
                        _message.value = "Massa poc àudio a '$phaseName'. Intenta-ho de nou."
                        _trainState.value = TrainState.Idle
                        return@launch
                    }

                    val centroid = FloatArray(MfccExtractor.NUM_COEFFICIENTS) { i ->
                        allMfcc.sumOf { it[i].toDouble() }.toFloat() / allMfcc.size
                    }
                    centroids.add(centroid)
                }

                _trainState.value = TrainState.MultiDone(centroids)

            } catch (e: Exception) {
                _message.value = "Error durant l'entrenament."
                _trainState.value = TrainState.Idle
            }
        }
    }

    fun saveNewVoiceMulti(name: String, centroids: List<FloatArray>) {
        if (centroids.isEmpty()) return
        viewModelScope.launch {
            val profile = VoiceProfile(
                id              = repository.newId(),
                name            = name.ifBlank { "Instrument sense nom" },
                mfccCentroid    = centroids[0],          // greu = centroid principal
                sampleCount     = 1,
                extraCentroids  = centroids.drop(1)      // mig + agut
            )
            repository.saveProfile(profile)
            _message.value = "Veu '${profile.name}' guardada amb ${centroids.size} registres!"
            _trainState.value = TrainState.Idle
        }
    }

    fun saveNewVoice(name: String, features: FloatArray, engineType: EngineType = EngineType.MFCC) {
        viewModelScope.launch {
            val engineLabel = if (engineType == EngineType.TENSORFLOW) " [TF]" else ""
            val profile = VoiceProfile(
                id           = repository.newId(),
                name         = name.ifBlank { "Instrument sense nom" },
                engineType   = engineType,
                mfccCentroid = features,
                sampleCount  = 1
            )
            repository.saveProfile(profile)
            _message.value = "Instrument '${profile.name}'$engineLabel guardat!"
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
