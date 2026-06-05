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

data class DynamicRangeScale(val min: Float, val max: Float)  // en escala 0-100

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _profileName = MutableStateFlow("Casa")
    val profileName: StateFlow<String> = _profileName

    private val _ranges = MutableStateFlow<Map<DynamicLevel, DynamicRangeScale>>(emptyMap())
    val ranges: StateFlow<Map<DynamicLevel, DynamicRangeScale>> = _ranges

    init {
        loadCurrentProfile()
    }

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
            _ranges.value = scaleRanges
        }
    }

    fun updateRange(level: DynamicLevel, min: Float, max: Float) {
        val current = _ranges.value.toMutableMap()
        current[level] = DynamicRangeScale(min, max)
        _ranges.value = current
        // Guardem immediatament (es pot millorar amb un botó "Desar")
        viewModelScope.launch {
            val dbMin = ScaleConverter.scaleToDb(min)
            val dbMax = ScaleConverter.scaleToDb(max)
            repository.saveRange(_profileName.value, level, DynamicRange(dbMin, dbMax))
        }
    }
}