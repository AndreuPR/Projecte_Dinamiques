package com.example.dinamiqapp.ui.voices

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinamiqapp.data.VoiceProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: VoiceManagementViewModel = viewModel()
    val profiles    by vm.profiles.collectAsState()
    val activeId    by vm.activeVoiceId.collectAsState()
    val trainState  by vm.trainState.collectAsState()
    val message     by vm.message.collectAsState()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Estat per al diàleg de nova veu
    var showNewVoiceDialog by remember { mutableStateOf(false) }
    var newVoiceName       by remember { mutableStateOf("") }
    // ID de la veu que estem millorant (null = cap)
    var improvingId        by remember { mutableStateOf<String?>(null) }

    // Superposició d'entrenament
    if (trainState !is TrainState.Idle) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                when (val ts = trainState) {
                    is TrainState.Countdown -> {
                        Text("Prepara't...", color = Color.White, fontSize = 22.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("${ts.secondsLeft}", color = Color(0xFFFF9800), fontSize = 96.sp, fontWeight = FontWeight.Bold)
                        Text("Toca el teu instrument!", color = Color.White.copy(alpha = 0.7f))
                    }
                    is TrainState.Recording -> {
                        Text("Enregistrant...", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("${ts.secondsLeft}s", color = Color(0xFFFFC107), fontSize = 72.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = Color(0xFFFFC107))
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(onClick = { vm.cancelTraining() }) {
                            Text("Cancel·lar", color = Color.White)
                        }
                    }
                    is TrainState.Done -> {
                        Text("Llest!", color = Color(0xFF4CAF50), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Dóna-li un nom a la veu/instrument:", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newVoiceName,
                            onValueChange = { newVoiceName = it },
                            label = { Text("Nom", color = Color.White) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.saveNewVoice(newVoiceName, ts.mfcc); newVoiceName = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Guardar veu") }
                    }
                    else -> {}
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestió de veus / instruments") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        },
        floatingActionButton = {
            if (hasPermission) {
                ExtendedFloatingActionButton(
                    onClick = { showNewVoiceDialog = true },
                    containerColor = Color(0xFF7B1FA2)
                ) {
                    Text("+ Nova veu", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("Cal el permís de micròfon per entrenar veus.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text("Concedir permís")
                        }
                    }
                }
                return@Scaffold
            }

            message?.let { msg ->
                Surface(color = if (msg.contains("Error")) MaterialTheme.colorScheme.errorContainer
                               else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()) {
                    Text(msg, modifier = Modifier.padding(12.dp),
                         color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cap veu entrenada", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Prem + Nova veu per entrenar el teu primer instrument",
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp),
                           verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(profiles, key = { it.id }) { profile ->
                        VoiceCard(
                            profile   = profile,
                            isActive  = profile.id == activeId,
                            onSelect  = { vm.setActiveVoice(profile.id) },
                            onImprove = { improvingId = profile.id },
                            onDelete  = { vm.deleteVoice(profile.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // espai per al FAB
                }
            }
        }
    }

    // Diàleg: nova veu (escull durada)
    if (showNewVoiceDialog) {
        var duration by remember { mutableIntStateOf(10) }
        AlertDialog(
            onDismissRequest = { showNewVoiceDialog = false },
            title = { Text("Nova veu / instrument") },
            text = {
                Column {
                    Text("Tria la durada de la gravació. Toca el teu instrument de forma variada (pp a ff).")
                    Spacer(Modifier.height(16.dp))
                    Text("Durada: ${duration}s", fontWeight = FontWeight.Medium)
                    Slider(value = duration.toFloat(), onValueChange = { duration = it.toInt() },
                           valueRange = 5f..30f, steps = 4)
                }
            },
            confirmButton = {
                Button(onClick = { showNewVoiceDialog = false; vm.startTraining(duration) }) {
                    Text("Iniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewVoiceDialog = false }) { Text("Cancel·lar") }
            }
        )
    }

    // Diàleg: millorar veu existent
    improvingId?.let { id ->
        var duration by remember { mutableIntStateOf(10) }
        AlertDialog(
            onDismissRequest = { improvingId = null },
            title = { Text("Millorar model") },
            text = {
                Column {
                    Text("Grava més mostres per millorar la precisió del model.")
                    Spacer(Modifier.height(16.dp))
                    Text("Durada: ${duration}s", fontWeight = FontWeight.Medium)
                    Slider(value = duration.toFloat(), onValueChange = { duration = it.toInt() },
                           valueRange = 5f..30f, steps = 4)
                }
            },
            confirmButton = {
                Button(onClick = { vm.improveVoice(id, duration); improvingId = null }) {
                    Text("Millorar")
                }
            },
            dismissButton = {
                TextButton(onClick = { improvingId = null }) { Text("Cancel·lar") }
            }
        )
    }
}

@Composable
private fun VoiceCard(
    profile: VoiceProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onImprove: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) Color(0xFF7B1FA2) else Color.Transparent
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 8.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${profile.sampleCount} session${if (profile.sampleCount == 1) "" else "s"} d'entrenament",
                         fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isActive) {
                    Text("ACTIU", color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) {
                    Button(onClick = onSelect, modifier = Modifier.weight(1f)) {
                        Text("Seleccionar")
                    }
                }
                OutlinedButton(onClick = onImprove, modifier = Modifier.weight(1f)) {
                    Text("Millorar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                         tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
