package com.example.dinamiqapp.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinamiqapp.data.DynamicLevel
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDynamicSettings: () -> Unit,
    onNavigateToHearingCalc: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val ranges by viewModel.ranges.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustar dinàmiques") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                }
            )
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
                val currentRange = ranges[level]
                if (currentRange != null) {
                    Text(
                        text = "${level.symbol} – ${level.fullName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Slider per al mínim
                    Text("Mínim: ${currentRange.min.toInt()}")
                    Slider(
                        value = currentRange.min,
                        onValueChange = { newMin ->
                            viewModel.updateRange(level, newMin, currentRange.max)
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Slider per al màxim
                    Text("Màxim: ${currentRange.max.toInt()}")
                    Slider(
                        value = currentRange.max,
                        onValueChange = { newMax ->
                            viewModel.updateRange(level, currentRange.min, newMax)
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}