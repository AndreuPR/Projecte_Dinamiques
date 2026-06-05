package com.example.dinamiqapp.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.data.DynamicLevel
import com.example.dinamiqapp.data.DynamicRange
import com.example.dinamiqapp.data.ScaleConverter
import com.example.dinamiqapp.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DynamicRangeScale(val min: Float, val max: Float)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _profileName = MutableStateFlow("Casa")
    val profileName: StateFlow<String> = _profileName

    // Esborrany local
    private val _draftPpMin = MutableStateFlow(0f)
    private val _draftPpMax = MutableStateFlow(20f)
    private val _draftPMax = MutableStateFlow(40f)
    private val _draftMfMax = MutableStateFlow(60f)
    private val _draftFMax = MutableStateFlow(80f)

    private val _draftMAX = MutableStateFlow(80f)

    // Rangs derivats per mostrar
    private val _ranges = MutableStateFlow<Map<DynamicLevel, DynamicRangeScale>>(emptyMap())
    val ranges: StateFlow<Map<DynamicLevel, DynamicRangeScale>> = _ranges

    init { loadCurrentProfile() }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            val name = repository.activeProfileName.first()
            _profileName.value = name
            val dbRanges = repository.profileRanges(name).first()
            val scaleRanges = dbRanges.mapValues { (_, range) ->
                DynamicRangeScale(
                    min = ScaleConverter.dbToScale(range.min),
                    max = ScaleConverter.dbToScale(range.max)
                )
            }
            _draftPpMin.value = scaleRanges[DynamicLevel.PP]?.min ?: 0f
            _draftPpMax.value = scaleRanges[DynamicLevel.PP]?.max ?: 20f
            _draftPMax.value = scaleRanges[DynamicLevel.P]?.max ?: 40f
            _draftMfMax.value = scaleRanges[DynamicLevel.MF]?.max ?: 60f
            _draftFMax.value = scaleRanges[DynamicLevel.F]?.max ?: 80f

            _draftMAX.value = scaleRanges[DynamicLevel.F]?.max ?: 99f
            recomputeRanges()
        }
    }

    private fun recomputeRanges() {
        val ppMin = _draftPpMin.value.coerceIn(0f, 99f)
        val ppMax = _draftPpMax.value.coerceIn(ppMin + 1f, 99f)
        val pMax  = _draftPMax.value.coerceIn(ppMax + 1f, 99f)
        val mfMax = _draftMfMax.value.coerceIn(pMax + 1f, 99f)
        val fMax  = _draftFMax.value.coerceIn(mfMax + 1f, 99f)
        val ffMax = 100f

        _draftPpMax.value = ppMax
        _draftPMax.value = pMax
        _draftMfMax.value = mfMax
        _draftFMax.value = fMax
        _draftMAX.value = ffMax


        _ranges.value = mapOf(
            DynamicLevel.PP to DynamicRangeScale(ppMin, ppMax),
            DynamicLevel.P  to DynamicRangeScale(ppMax + 1f, pMax),
            DynamicLevel.MF to DynamicRangeScale(pMax + 1f, mfMax),
            DynamicLevel.F  to DynamicRangeScale(mfMax + 1f, fMax),
            DynamicLevel.FF to DynamicRangeScale(fMax + 1f, ffMax)
        )
    }

    fun updatePpMin(min: Float) { _draftPpMin.value = min; recomputeRanges() }
    fun updatePpMax(max: Float) { _draftPpMax.value = max; recomputeRanges() }
    fun updatePMax(max: Float) { _draftPMax.value = max; recomputeRanges() }
    fun updateMfMax(max: Float) { _draftMfMax.value = max; recomputeRanges() }
    fun updateFMax(max: Float) { _draftFMax.value = max; recomputeRanges() }
    fun updateMax(max: Float) { _draftMAX.value = max ; recomputeRanges() }

    fun save() {
        viewModelScope.launch {
            val profileName = _profileName.value
            val ranges = _ranges.value
            for ((level, range) in ranges) {
                val dbMin = ScaleConverter.scaleToDb(range.min)
                val dbMax = ScaleConverter.scaleToDb(range.max)
                repository.saveRange(profileName, level, DynamicRange(dbMin, dbMax))
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val profileName = _profileName.value
            repository.resetToDefaults(profileName)
            // recarreguem els valors desats
            loadCurrentProfile()
        }
    }
}