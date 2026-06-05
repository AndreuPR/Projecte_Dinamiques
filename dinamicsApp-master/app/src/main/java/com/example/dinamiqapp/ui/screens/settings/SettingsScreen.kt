package com.example.dinamiqapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinamiqapp.data.DynamicLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val ranges by viewModel.ranges.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustar dinàmiques") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } })
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("Aplicar")
                }
                OutlinedButton(onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("Predeterminat")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DynamicLevel.values().forEach { level ->
                val range = ranges[level] ?: return@forEach

                Text(
                    text = "${level.symbol} – ${level.fullName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (level) {
                    DynamicLevel.PP -> {
                        Text("Mínim: ${range.min.toInt()}")
                        Slider(
                            value = range.min,
                            onValueChange = { viewModel.updatePpMin(it) },
                            valueRange = 0f..(range.max - 1f).coerceAtLeast(0f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Màxim: ${range.max.toInt()}")
                        Slider(
                            value = range.max,
                            onValueChange = { viewModel.updatePpMax(it) },
                            valueRange = (range.min + 1f)..99f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    DynamicLevel.FF -> {
                        Text("Mínim: ${range.min.toInt()} (automàtic)")
                        Text("Màxim: 100 (fix)")
                    }
                    else -> {
                        Text("Mínim: ${range.min.toInt()} (automàtic)")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Màxim: ${range.max.toInt()}")
                        Slider(
                            value = range.max,
                            onValueChange = { newMax ->
                                when (level) {
                                    DynamicLevel.P -> viewModel.updatePMax(newMax)
                                    DynamicLevel.MF -> viewModel.updateMfMax(newMax)
                                    DynamicLevel.F -> viewModel.updateFMax(newMax)
                                    else -> {} // No hauria d'arribar
                                }
                            },
                            valueRange = (range.min + 1f)..99f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}