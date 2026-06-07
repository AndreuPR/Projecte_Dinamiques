package com.example.dinamiqapp.ui.screens.appsettings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val viewModel: AppSettingsViewModel = viewModel()
    val minDb by viewModel.minDb.collectAsState()
    val maxDb by viewModel.maxDb.collectAsState()
    val refreshMs by viewModel.refreshMs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Paràmetres de l'app") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Límit inferior (dBFS): ${minDb.toInt()}")
            Slider(value = minDb, onValueChange = { viewModel.updateMinDb(it) },
                valueRange = -90f..0f, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
            Text("Límit superior (dBFS): ${maxDb.toInt()}")
            Slider(value = maxDb, onValueChange = { viewModel.updateMaxDb(it) },
                valueRange = -90f..0f, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
            Text("Temps de refresc (ms): $refreshMs")
            Slider(value = refreshMs.toFloat(),
                onValueChange = { viewModel.updateRefreshMs(it.toInt()) },
                valueRange = 50f..1000f, steps = 19, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.save(); onBack() },
                modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Desar i tornar")
            }
        }
    }
}