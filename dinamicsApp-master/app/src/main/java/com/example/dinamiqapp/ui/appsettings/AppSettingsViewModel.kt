package com.example.dinamiqapp.ui.screens.appsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinamiqapp.data.ScaleConverter
import com.example.dinamiqapp.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _minDb = MutableStateFlow(-70f)
    private val _maxDb = MutableStateFlow(0f)
    private val _refreshMs = MutableStateFlow(200)

    val minDb: StateFlow<Float> = _minDb
    val maxDb: StateFlow<Float> = _maxDb
    val refreshMs: StateFlow<Int> = _refreshMs

    init {
        viewModelScope.launch {
            repository.appMinDb.first().let { _minDb.value = it }
            repository.appMaxDb.first().let { _maxDb.value = it }
            repository.appRefreshMs.first().let { _refreshMs.value = it }
        }
    }

    fun updateMinDb(value: Float) { _minDb.value = value }
    fun updateMaxDb(value: Float) { _maxDb.value = value }
    fun updateRefreshMs(value: Int) { _refreshMs.value = value }

    fun save() {
        viewModelScope.launch {
            val min = _minDb.value.coerceIn(-90f, _maxDb.value - 1f)
            val max = _maxDb.value.coerceIn(min + 1f, 0f)
            val refresh = _refreshMs.value.coerceIn(50, 1000)
            repository.setAppMinDb(min)
            repository.setAppMaxDb(max)
            repository.setAppRefreshMs(refresh)
            // Actualitza el ScaleConverter global
            ScaleConverter.minDb = min
            ScaleConverter.maxDb = max
        }
    }
}