package com.example.dinamiqapp.ui.screens.hearing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.audio.FilteredAudioSource
import com.example.dinamiqapp.data.*
import com.example.dinamiqapp.data.VoiceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class RecordState {
    object Idle : RecordState()
    data class Countdown(val secondsLeft: Int) : RecordState()
    object Recording : RecordState()
    data class Recorded(val value: Float) : RecordState()   // valor en escala 0-100
    data class Calculated(val value: Float) : RecordState()
}

sealed class FlyState {
    object Idle : FlyState()
    data class Countdown(val secondsLeft: Int) : FlyState()
    data class Recording(val secondsLeft: Int, val liveDb: Float?, val minDb: Float?, val maxDb: Float?) : FlyState()
}

class HearingCalcViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    companion object {
        const val LISTEN_DURATION_SECONDS = 3f
        const val COUNTDOWN_SECONDS = 3
        private const val RANGE_MAX = 100f
    }

    // Percentils llegits del DataStore (valors per defecte fins que es carreguen)
    private var ppPercentile = 0.15f
    private var ffPercentile = 0.90f

    init {
        viewModelScope.launch {
            ppPercentile = repository.signalPpPercentile.first()
            ffPercentile = repository.signalFfPercentile.first()
            // Carrega el model de veu actiu → tota la calibració passa pel filtre
            FilteredAudioSource.setVoice(VoiceRepository(application).activeProfile())
        }
    }

    private fun percentile(values: List<Float>, p: Float): Float {
        val sorted = values.sorted()
        val idx = (p * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }

    private val _states = MutableStateFlow<Map<DynamicLevel, RecordState>>(
        DynamicLevel.values().associateWith { RecordState.Idle }
    )
    val states: StateFlow<Map<DynamicLevel, RecordState>> = _states

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Per mostrar el mesurador en temps real durant l'enregistrament
    private val _liveDb = MutableStateFlow<Float?>(null)   // en escala 0-100
    val liveDb: StateFlow<Float?> = _liveDb

    private val _recordingLevel = MutableStateFlow<DynamicLevel?>(null)
    val recordingLevel: StateFlow<DynamicLevel?> = _recordingLevel

    private val _flyState = MutableStateFlow<FlyState>(FlyState.Idle)
    val flyState: StateFlow<FlyState> = _flyState

    private var flyJob: Job? = null

    private var recordingJob: Job? = null
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startRecording(level: DynamicLevel) {
        if (recordingJob?.isActive == true) return

        if (_states.value[level] is RecordState.Recorded) {
            _errorMessage.value = "${level.symbol} ja té un valor enregistrat."
            return
        }

        _errorMessage.value = null
        _recordingLevel.value = level

        recordingJob = viewModelScope.launch {
            try {
                // Compte enrere abans de gravar
                for (s in COUNTDOWN_SECONDS downTo 1) {
                    updateState(level, RecordState.Countdown(s))
                    delay(1000L)
                }
                updateState(level, RecordState.Recording)

                val values = mutableListOf<Float>()
                val startTime = System.currentTimeMillis()
                val durationMs = (LISTEN_DURATION_SECONDS * 1000).toLong()

                FilteredAudioSource.filteredDbFlow()
                    .takeWhile { System.currentTimeMillis() - startTime < durationMs }
                    .collect { db ->
                        values.add(db)
                        _liveDb.value = ScaleConverter.dbToScale(db)
                    }

                if (values.isNotEmpty()) {
                    val scaleValues = values.map { ScaleConverter.dbToScale(it) }
                    // PP: agafem el percentil 25 (ignora silencis inicials)
                    // FF: agafem el percentil 90 (ignora pics accidentals)
                    // Resta: mitjana central (percentil 50)
                    val representativeScale = when (level) {
                        DynamicLevel.PP -> percentile(scaleValues, ppPercentile)
                        DynamicLevel.FF -> percentile(scaleValues, ffPercentile)
                        else -> percentile(scaleValues, 0.50f)
                    }
                    val rounded = representativeScale.coerceIn(0f, 100f)

                    val previousMax = getPreviousMax(level)
                    if (level != DynamicLevel.PP && rounded <= previousMax) {
                        _errorMessage.value = "El valor de ${level.symbol} ha de ser superior a ${
                            previousMax.toInt()
                        } (màxim de la dinàmica anterior)."
                        updateState(level, RecordState.Idle)
                    } else {
                        updateState(level, RecordState.Recorded(rounded))
                    }
                } else {
                    updateState(level, RecordState.Idle)
                    _errorMessage.value = "No s'ha pogut capturar cap valor."
                }
            } catch (e: Exception) {
                updateState(level, RecordState.Idle)
                _errorMessage.value = "Error durant l'enregistrament."
            } finally {
                _recordingLevel.value = null
                _liveDb.value = null
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startFlyRecording(durationSeconds: Int) {
        if (flyJob?.isActive == true || recordingJob?.isActive == true) return
        _errorMessage.value = null

        flyJob = viewModelScope.launch {
            try {
                // Compte enrere
                for (s in COUNTDOWN_SECONDS downTo 1) {
                    _flyState.value = FlyState.Countdown(s)
                    delay(1000L)
                }

                // Gravació: acumula tots els valors per calcular percentils al final
                val allScales = mutableListOf<Float>()
                val startTime = System.currentTimeMillis()
                val durationMs = durationSeconds * 1000L

                FilteredAudioSource.filteredDbFlow()
                    .takeWhile { System.currentTimeMillis() - startTime < durationMs }
                    .collect { db ->
                        val scale = ScaleConverter.dbToScale(db)
                        allScales.add(scale)
                        // Per mostrar en directe: percentil actual de pp i ff
                        val liveMin = if (allScales.size > 5) percentile(allScales, ppPercentile) else null
                        val liveMax = if (allScales.size > 5) percentile(allScales, ffPercentile) else null
                        val secondsLeft = ((durationMs - (System.currentTimeMillis() - startTime)) / 1000L).toInt() + 1
                        _flyState.value = FlyState.Recording(secondsLeft, scale, liveMin, liveMax)
                    }

                // Calcula percentils finals
                if (allScales.size < 10) {
                    _errorMessage.value = "Massa poc àudio capturat. Intenta-ho de nou."
                    _flyState.value = FlyState.Idle
                    return@launch
                }

                val mn = percentile(allScales, ppPercentile)
                val mx = percentile(allScales, ffPercentile)

                if (mx - mn < 2f) {
                    _errorMessage.value = "Rang massa petit. Toca des de pp fins a ff!"
                    _flyState.value = FlyState.Idle
                    return@launch
                }

                val step = (mx - mn) / 4f
                val updated = _states.value.toMutableMap()
                updated[DynamicLevel.PP] = RecordState.Calculated(mn + step)
                updated[DynamicLevel.P]  = RecordState.Calculated(mn + step * 2f)
                updated[DynamicLevel.MF] = RecordState.Calculated(mn + step * 3f)
                updated[DynamicLevel.F]  = RecordState.Calculated(mn + step * 4f - 1f)
                updated[DynamicLevel.FF] = RecordState.Calculated(mx)
                _states.value = updated

            } catch (e: Exception) {
                _errorMessage.value = "Error durant la gravació al vol."
            } finally {
                _flyState.value = FlyState.Idle
            }
        }
    }

    private fun getPreviousMax(level: DynamicLevel): Float {
        val ordered = DynamicLevel.values()
        val idx = ordered.indexOf(level)
        if (idx <= 0) return -1f
        // Mirem el màxim de la dinàmica anterior segons els estats actuals
        val prevLevel = ordered[idx - 1]
        val prevState = _states.value[prevLevel]
        return when (prevState) {
            is RecordState.Recorded -> {
                // Calculem el seu màxim segons la regla del 75%
                computeMax(prevLevel, prevState.value)
            }
            is RecordState.Calculated -> prevState.value  // ja és el màxim calculat
            else -> 0f
        }
    }

    // Calcula el màxim d'una dinàmica a partir del valor enregistrat (excepte FF)
    private fun computeMax(level: DynamicLevel, recordedValue: Float): Float {
        if (level == DynamicLevel.FF) return RANGE_MAX
        val min = if (level == DynamicLevel.PP) 0f else getPreviousMax(level)
        // valor = min + 0.75 * (max - min)  =>  max = min + (valor - min) / 0.75
        val maxCandidate = min + (recordedValue - min) / 0.75f
        return maxCandidate.coerceIn(min + 1f, RANGE_MAX - 1f)
    }

    fun calculateIntermediates() {
        _errorMessage.value = null
        val currentStates = _states.value

        val ppVal = (currentStates[DynamicLevel.PP] as? RecordState.Recorded)?.value
        val ffVal = (currentStates[DynamicLevel.FF] as? RecordState.Recorded)?.value
        if (ppVal == null || ffVal == null) {
            _errorMessage.value = "Cal enregistrar pp i ff"
            return
        }
        if (ffVal <= ppVal) {
            _errorMessage.value = "ff ha de ser més gran que pp"
            return
        }

        // Recollim totes les gravacions disponibles
        val recorded = mutableListOf<Pair<DynamicLevel, Float>>()
        DynamicLevel.values().forEach { level ->
            val state = currentStates[level]
            if (state is RecordState.Recorded) recorded.add(level to state.value)
        }

        val updated = currentStates.toMutableMap()
        if (recorded.size == 2) {
            // Només PP i FF – repartiment uniforme
            val totalGap = ffVal - ppVal
            val step = totalGap / 4f
            updated[DynamicLevel.PP] = RecordState.Calculated(ppVal)
            updated[DynamicLevel.P]  = RecordState.Calculated(ppVal + step)
            updated[DynamicLevel.MF] = RecordState.Calculated(ppVal + 2 * step)
            updated[DynamicLevel.F]  = RecordState.Calculated(ppVal + 3 * step)
            updated[DynamicLevel.FF] = RecordState.Calculated(ffVal)
        } else {
            // Totes o algunes – mètode dels punts mitjos
            // Ordenem per ordre natural (PP, P, MF, F, FF)
            val ordered = DynamicLevel.values().toList()
            var previousValue = 0f   // per a PP, el mínim és 0
            var previousLevel: DynamicLevel? = null
            for (level in ordered) {
                val currentValue = when (level) {
                    DynamicLevel.PP -> ppVal
                    DynamicLevel.FF -> ffVal
                    else -> (currentStates[level] as? RecordState.Recorded)?.value
                }
                if (currentValue != null) {
                    if (previousLevel != null) {
                        // El límit entre l'anterior i aquest és el punt mig
                        val boundary = (previousValue + currentValue) / 2f
                        updated[previousLevel] = RecordState.Calculated(boundary)
                    }
                    previousValue = currentValue
                    previousLevel = level
                }
            }
            // L'últim nivell (FF) té màxim 100
            updated[DynamicLevel.FF] = RecordState.Calculated(100f)
        }

        _states.value = updated
    }

    fun applyToProfile() {
        viewModelScope.launch {
            val profileName = repository.activeProfileName.first()
            val currentStates = _states.value
            var previousMax = 0f
            for (level in DynamicLevel.values()) {
                val state = currentStates[level]
                val max = when (state) {
                    is RecordState.Calculated -> state.value
                    is RecordState.Recorded -> {
                        // Si no s'ha calculat, fem servir el valor enregistrat com a màxim temporal
                        state.value
                    }
                    else -> continue
                }
                val min = if (level == DynamicLevel.PP) 0f else previousMax + 1f
                val dbMin = ScaleConverter.scaleToDb(min)
                val dbMax = ScaleConverter.scaleToDb(max.coerceIn(min + 1f, 100f))
                repository.saveRange(profileName, level, DynamicRange(dbMin, dbMax))
                previousMax = max
            }
            // Actualitza l'escala perquè reflecteixi el nou rang calibrat
            val updatedRanges = repository.profileRanges(profileName).first()
            ScaleConverter.updateFromRanges(updatedRanges)
            _errorMessage.value = "Rangs aplicats!"
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val profileName = repository.activeProfileName.first()
            repository.resetToDefaults(profileName)
            _states.value = DynamicLevel.values().associateWith { RecordState.Idle }
            _errorMessage.value = "Valors predeterminats restaurats."
        }
    }

    private fun updateState(level: DynamicLevel, state: RecordState) {
        _states.value = _states.value.toMutableMap().apply { put(level, state) }
    }
}