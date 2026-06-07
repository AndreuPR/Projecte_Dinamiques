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
import com.example.dinamiqapp.audio.engines.EngineType
import com.example.dinamiqapp.data.VoiceProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: VoiceManagementViewModel = viewModel()
    val profiles       by vm.profiles.collectAsState()
    val activeId       by vm.activeVoiceId.collectAsState()
    val trainState     by vm.trainState.collectAsState()
    val message        by vm.message.collectAsState()
    val isTfAvailable  = vm.isTfModelAvailable

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
                        if (ts.phase.isNotEmpty()) {
                            Text(ts.phase, color = Color(0xFFCE93D8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text("Prepara't...", color = Color.White, fontSize = 22.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("${ts.secondsLeft}", color = Color(0xFFFF9800), fontSize = 96.sp, fontWeight = FontWeight.Bold)
                        Text("Toca el teu instrument!", color = Color.White.copy(alpha = 0.7f))
                    }
                    is TrainState.Recording -> {
                        if (ts.phase.isNotEmpty()) {
                            Text(ts.phase, color = Color(0xFFCE93D8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                        }
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
                        val engineColor = if (ts.engineType == EngineType.TENSORFLOW)
                            Color(0xFF00796B) else Color(0xFF7B1FA2)
                        val engineLabel = if (ts.engineType == EngineType.TENSORFLOW)
                            "Motor: TensorFlow Lite" else "Motor: MFCC"
                        Text("Llest!", color = Color(0xFF4CAF50), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(engineLabel, color = engineColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(16.dp))
                        Text("Dóna-li un nom a l'instrument:", color = Color.White)
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
                            onClick = { vm.saveNewVoice(newVoiceName, ts.features, ts.engineType); newVoiceName = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Guardar instrument") }
                    }
                    is TrainState.MultiDone -> {
                        Text("✓ 3 registres capturats!", color = Color(0xFF4CAF50), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Greu · Mig · Agut", color = Color(0xFFCE93D8), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Dóna-li un nom a l'instrument:", color = Color.White)
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
                            onClick = { vm.saveNewVoiceMulti(newVoiceName, ts.centroids); newVoiceName = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Guardar instrument") }
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

    // Diàleg: nova veu (escull motor i mode d'entrenament)
    if (showNewVoiceDialog) {
        var duration      by remember { mutableIntStateOf(10) }
        var multiRegister by remember { mutableStateOf(true) }
        var useTf         by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showNewVoiceDialog = false },
            title = { Text("Nova veu / instrument") },
            text = {
                Column {
                    // ── Elecció de motor ──────────────────────────────────
                    Text("Motor de reconeixement", fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))

                    // MFCC
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (!useTf) Color(0xFF7B1FA2).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = !useTf, onClick = { useTf = false })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("MFCC", fontWeight = FontWeight.Bold,
                                     color = Color(0xFF7B1FA2))
                                Text("Ràpid, sense configuració addicional. " +
                                     "Millora molt entrenant per registres.",
                                     style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // TensorFlow Lite
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (useTf) Color(0xFF00796B).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = useTf,
                                onClick  = { useTf = true },
                                enabled  = isTfAvailable
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("TensorFlow Lite", fontWeight = FontWeight.Bold,
                                         color = if (isTfAvailable) Color(0xFF00796B)
                                                 else MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!isTfAvailable) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("(yamnet.tflite no trobat)",
                                             style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Text("Molt més precís (xarxa neuronal). " +
                                     "Funciona bé en tots els registres. " +
                                     "Recomana entrenar 30-60s.",
                                     style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Mode d'entrenament (només per a MFCC) ────────────
                    if (!useTf) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            Switch(checked = multiRegister, onCheckedChange = { multiRegister = it })
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (multiRegister) "Per registres (recomanat)" else "Gravació única",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (multiRegister) "Greu · Mig · Agut per separat"
                                    else "Toca de pp a ff tot seguit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(
                            "Toca tot el teu rang (greu i agut, pp i ff). " +
                            "Com més variació i durada, millor resultat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Durada ────────────────────────────────────────────
                    val maxDuration = if (useTf) 60 else 20
                    val minDuration = if (useTf) 20 else 5
                    if (duration < minDuration) duration = minDuration
                    Text(
                        if (!useTf && multiRegister)
                            "Durada per registre: ${duration}s (total ~${duration * 3}s)"
                        else
                            "Durada: ${duration}s",
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = duration.toFloat(),
                        onValueChange = { duration = it.toInt() },
                        valueRange = minDuration.toFloat()..maxDuration.toFloat(),
                        steps = if (useTf) 3 else 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showNewVoiceDialog = false
                    when {
                        useTf         -> vm.startTraining(EngineType.TENSORFLOW, duration)
                        multiRegister -> vm.startMultiRegisterTraining(duration)
                        else          -> vm.startTraining(EngineType.MFCC, duration)
                    }
                }) { Text("Iniciar") }
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
    // Color per motor: lila = MFCC, verd fosc = TF Lite
    val engineColor = if (profile.engineType == EngineType.TENSORFLOW)
        Color(0xFF00796B) else Color(0xFF7B1FA2)
    val borderColor = if (isActive) engineColor else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 8.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    val registersText = if (profile.hasMultipleRegisters)
                        " · ${profile.allCentroids.size} registres" else ""
                    val engineTag = if (profile.engineType == EngineType.TENSORFLOW) " · TF Lite" else " · MFCC"
                    Text("${profile.sampleCount} session${if (profile.sampleCount == 1) "" else "s"}$registersText$engineTag",
                         fontSize = 12.sp, color = engineColor)
                }
                if (isActive) {
                    Text("ACTIU", color = engineColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
