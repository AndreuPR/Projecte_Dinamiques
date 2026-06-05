package com.example.cridar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val minDb by viewModel.minDb.collectAsState()
    val maxDb by viewModel.maxDb.collectAsState()
    val refreshMs by viewModel.refreshMs.collectAsState()
    val threshold by viewModel.threshold.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configuració") }, navigationIcon = { TextButton(onClick = onBack) { Text("←") } })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Límit inferior dBFS: ${minDb.toInt()}")
            Slider(value = minDb, onValueChange = { viewModel.updateMinDb(it) }, valueRange = -90f..0f)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Límit superior dBFS: ${maxDb.toInt()}")
            Slider(value = maxDb, onValueChange = { viewModel.updateMaxDb(it) }, valueRange = -90f..0f)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Temps de refresc (ms): $refreshMs")
            Slider(value = refreshMs.toFloat(), onValueChange = { viewModel.updateRefreshMs(it.toInt()) }, valueRange = 50f..1000f, steps = 19)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Llindar de crit (0-100): ${threshold.toInt()}")
            Slider(value = threshold, onValueChange = { viewModel.updateThreshold(it) }, valueRange = 0f..100f)

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Veu robòtica: ")
                Switch(checked = voiceEnabled, onCheckedChange = { viewModel.updateVoiceEnabled(it) })
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.save(); onBack() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Desar i tornar")
            }
        }
    }
}