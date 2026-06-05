package com.example.dinamiqapp.ui.screens.hearing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.AudioMeter
import com.example.dinamiqapp.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Estat de cada botó dinàmic
sealed class RecordState {
    object Idle : RecordState()
    object Recording : RecordState()
    data class Recorded(val value: Float) : RecordState()  // valor 0-100
    data class Calculated(val value: Float) : RecordState()
}

class HearingCalcViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    companion object {
        const val LISTEN_DURATION_SECONDS = 3f   // ajustable aquí
        private const val DEFAULT_RANGE_WIDTH = 8f  // amplada del rang en escala 0-100
    }

    // Estat de cada nivell
    private val _states = MutableStateFlow<Map<DynamicLevel, RecordState>>(
        DynamicLevel.values().associateWith { RecordState.Idle }
    )
    val states: StateFlow<Map<DynamicLevel, RecordState>> = _states

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var recordingJob: Job? = null

    fun startRecording(level: DynamicLevel) {
        // Evita començar un nou enregistrament si ja n’hi ha un en curs
        if (recordingJob?.isActive == true) return

        // Marca com a Recording
        updateState(level, RecordState.Recording)

        recordingJob = viewModelScope.launch  {
            try {
                // Recull dBFS durant LISTEN_DURATION_SECONDS
                val values = mutableListOf<Float>()
                val startTime = System.currentTimeMillis()
                val durationMs = (LISTEN_DURATION_SECONDS * 1000).toLong()

                AudioMeter.dbFlow()
                    .takeWhile { System.currentTimeMillis() - startTime < durationMs }
                    .collect { db ->
                        values.add(db)
                    }

                if (values.isNotEmpty()) {
                    // Converteix cada dB a escala 0-100 i fa la mitjana
                    val avgDb = values.average().toFloat()
                    val avgScale = ScaleConverter.dbToScale(avgDb)
                    updateState(level, RecordState.Recorded(avgScale))
                } else {
                    updateState(level, RecordState.Idle)
                }
            } catch (e: Exception) {
                updateState(level, RecordState.Idle)
            }
        }
    }

    fun calculateIntermediates() {
        _errorMessage.value = null
        val currentStates = _states.value
        val ppState = currentStates[DynamicLevel.PP]
        val ffState = currentStates[DynamicLevel.FF]

        // Comprova que PP i FF tinguin un valor registrat
        if (ppState !is RecordState.Recorded || ffState !is RecordState.Recorded) {
            val missing = mutableListOf<String>()
            if (ppState !is RecordState.Recorded) missing.add("pp")
            if (ffState !is RecordState.Recorded) missing.add("ff")
            _errorMessage.value = "Cal enregistrar: ${missing.joinToString(", ")}"
            return
        }

        val ppVal = ppState.value
        val ffVal = ffState.value

        // Interpolació lineal per als nivells entremig
        val step = (ffVal - ppVal) / 4f
        val pVal  = ppVal + step
        val mfVal = ppVal + 2 * step
        val fVal  = ppVal + 3 * step

        // Actualitza els estats: si no estaven ja enregistrats, els marca com a Calculated
        val updated = currentStates.toMutableMap()
        listOf(
            DynamicLevel.P to pVal,
            DynamicLevel.MF to mfVal,
            DynamicLevel.F to fVal
        ).forEach { (level, value) ->
            if (updated[level] !is RecordState.Recorded) {
                updated[level] = RecordState.Calculated(value)
            }
        }
        _states.value = updated
    }

    fun applyToProfile() {
        viewModelScope.launch {
            val profileName = repository.activeProfileName.first()
            val currentStates = _states.value
            for (level in DynamicLevel.values()) {
                val state = currentStates[level] ?: continue
                val center = when (state) {
                    is RecordState.Recorded -> state.value
                    is RecordState.Calculated -> state.value
                    else -> continue
                }
                val halfWidth = DEFAULT_RANGE_WIDTH / 2f
                val min = (center - halfWidth).coerceIn(0f, 100f)
                val max = (center + halfWidth).coerceIn(0f, 100f)
                val dbMin = ScaleConverter.scaleToDb(min)
                val dbMax = ScaleConverter.scaleToDb(max)
                repository.saveRange(profileName, level, DynamicRange(dbMin, dbMax))
            }
            _errorMessage.value = "Rangs aplicats!"
        }
    }

    suspend fun resetToDefaults(profileName: String) {
        val defaults = if (profileName == "Concert") DefaultProfiles.CONCERT else DefaultProfiles.HOME
        defaults.ranges.forEach { (level, range) ->
            saveRange(profileName, level, range)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun updateState(level: DynamicLevel, state: RecordState) {
        _states.value = _states.value.toMutableMap().apply { put(level, state) }
    }
}
