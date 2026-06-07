package com.example.dinamiqapp.ui.screens.appsettings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val viewModel: AppSettingsViewModel = viewModel()
    val minDb by viewModel.minDb.collectAsState()
    val maxDb by viewModel.maxDb.collectAsState()
    val refreshMs by viewModel.refreshMs.collectAsState()
    val emaAlpha by viewModel.emaAlpha.collectAsState()
    val hysteresis by viewModel.hysteresis.collectAsState()
    val ppPercentile by viewModel.ppPercentile.collectAsState()
    val ffPercentile by viewModel.ffPercentile.collectAsState()
    val keepLearning by viewModel.keepLearning.collectAsState()
    val similarityThreshold by viewModel.similarityThreshold.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paràmetres de l'app") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Secció: Micròfon ──────────────────────────────────────
            SectionTitle("Micròfon")
            Text(
                "Rang de dBFS: defineix la sensibilitat global del micròfon. " +
                "Funciona com a fallback quan no hi ha perfil calibrat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ParamSlider(
                label = "Límit inferior (dBFS)",
                value = minDb,
                valueText = "${minDb.toInt()} dB",
                onValueChange = { viewModel.updateMinDb(it) },
                range = -90f..(-1f)
            )
            ParamSlider(
                label = "Límit superior (dBFS)",
                value = maxDb,
                valueText = "${maxDb.toInt()} dB",
                onValueChange = { viewModel.updateMaxDb(it) },
                range = -89f..0f
            )
            ParamSlider(
                label = "Freqüència d'escolta",
                value = refreshMs.toFloat(),
                valueText = "${refreshMs} ms",
                onValueChange = { viewModel.updateRefreshMs(it.toInt()) },
                range = 50f..1000f,
                steps = 19
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Secció: Motor de senyal ───────────────────────────────
            SectionTitle("Motor de senyal")
            Text(
                "Controla com es processa el so en temps real. " +
                "Aquests paràmetres afecten l'estabilitat i la velocitat de resposta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ParamSlider(
                label = "Suavitzat (EMA alpha)",
                value = emaAlpha,
                valueText = "%.2f".format(emaAlpha) +
                    when {
                        emaAlpha < 0.15f -> " — molt suau"
                        emaAlpha < 0.35f -> " — suau"
                        emaAlpha < 0.65f -> " — mig"
                        else             -> " — ràpid"
                    },
                onValueChange = { viewModel.updateEmaAlpha(it) },
                range = 0.05f..1.0f
            )

            ParamSlider(
                label = "Histèresi (lectures per confirmar)",
                value = hysteresis.toFloat(),
                valueText = "$hysteresis lectures" +
                    when {
                        hysteresis <= 1 -> " — sense filtre"
                        hysteresis <= 3 -> " — lleuger"
                        hysteresis <= 6 -> " — mig"
                        else            -> " — estable"
                    },
                onValueChange = { viewModel.updateHysteresis(it.toInt()) },
                range = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Secció: Calibratge ────────────────────────────────────
            SectionTitle("Calibratge (percentils)")
            Text(
                "Controla quins valors es consideren pp i ff durant el calibratge. " +
                "Percentils baixos de pp ignoren el soroll ambient.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ParamSlider(
                label = "Percentil pp (mínim)",
                value = ppPercentile,
                valueText = "${(ppPercentile * 100).toInt()}%" +
                    " — ignora el ${(ppPercentile * 100).toInt()}% de valors més baixos",
                onValueChange = { viewModel.updatePpPercentile(it) },
                range = 0.05f..0.40f
            )
            ParamSlider(
                label = "Percentil ff (màxim)",
                value = ffPercentile,
                valueText = "${(ffPercentile * 100).toInt()}%" +
                    " — agafa el ${(ffPercentile * 100).toInt()}% dels valors",
                onValueChange = { viewModel.updateFfPercentile(it) },
                range = 0.60f..0.99f
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Secció: Reconeixement de veu ──────────────────────────
            SectionTitle("Reconeixement de veu")
            Text(
                "Si 'Seguir aprenent' està actiu, l'app millora el model de veu en segon pla " +
                "mentre mesures. Consumeix una mica més de CPU.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ParamSlider(
                label = "Llindar de similitud",
                value = similarityThreshold,
                valueText = "%.2f".format(similarityThreshold) +
                    when {
                        similarityThreshold < 0.65f -> " — molt permissiu (pot filtrar poc)"
                        similarityThreshold < 0.75f -> " — permissiu"
                        similarityThreshold < 0.85f -> " — equilibrat"
                        similarityThreshold < 0.92f -> " — estricte"
                        else                        -> " — molt estricte"
                    },
                onValueChange = { viewModel.updateSimilarityThreshold(it) },
                range = 0.50f..0.99f
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Seguir aprenent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(if (keepLearning) "Actiu — millorant el model" else "Inactiu — estalvi de recursos",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = keepLearning, onCheckedChange = { viewModel.updateKeepLearning(it) })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.save(); onBack() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Desar i tornar", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        steps = steps,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
}
