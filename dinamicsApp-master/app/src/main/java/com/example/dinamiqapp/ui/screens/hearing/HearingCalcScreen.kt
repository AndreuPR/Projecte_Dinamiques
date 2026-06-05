package com.example.dinamiqapp.ui.screens.hearing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinamiqapp.data.DynamicLevel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingCalcScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HearingCalcViewModel = viewModel()
    val states by viewModel.states.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val liveDb by viewModel.liveDb.collectAsState()
    val recordingLevel by viewModel.recordingLevel.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cal el permís de micròfon per a l'enregistrament.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Concedir permís")
                }
            }
        }
        return
    }

    // Superposició d'enregistrament
    if (recordingLevel != null) {
        androidx.compose.ui.platform.LocalView.current.keepScreenOn = true
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enregistrant ${recordingLevel?.symbol}...",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Mesurador en directe
                Text(
                    text = if (liveDb != null) "${liveDb!!.toInt()}" else "--",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Intensitat", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(32.dp))
                // Cercle animat senzill (progrés)
                CircularProgressIndicator(color = Color.White)
            }
        }
        return
    }

    // Pantalla principal
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Càlcul per audició") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DynamicLevel.values().forEach { level ->
                val state = states[level] ?: RecordState.Idle
                val buttonColor = when (state) {
                    is RecordState.Recorded -> Color(0xFF4CAF50)
                    is RecordState.Calculated -> Color(0xFF2196F3)
                    is RecordState.Recording -> Color(0xFFFFC107)
                    is RecordState.Idle -> MaterialTheme.colorScheme.primary
                }
                val text = when (state) {
                    is RecordState.Recorded -> "${level.symbol} (${state.value.toInt()})"
                    is RecordState.Calculated -> "${level.symbol} (max ${state.value.toInt()})"
                    is RecordState.Recording -> "${level.symbol} (Escoltant…)"
                    is RecordState.Idle -> level.symbol
                }
                Button(
                    onClick = { viewModel.startRecording(level) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    enabled = state !is RecordState.Recording
                ) {
                    Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.calculateIntermediates() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Calcular resta", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.applyToProfile() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Aplicar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Predeterminat", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}