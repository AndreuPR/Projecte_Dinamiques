package com.example.cridar.ui.screens.listening


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cridar.MainActivity
import com.example.cridar.ui.screens.listening.ListeningViewModel
import kotlinx.coroutines.delay

@Composable
fun ListeningScreen(
    onShoutDetected: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ListeningViewModel = viewModel()
    val isShouting by viewModel.isShouting.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    LaunchedEffect(isShouting) {
        if (isShouting) {
            if (voiceEnabled) {
                // accedim al TTS (podem obtenir-lo de LocalContext.current)
                (context as? android.app.Activity)?.let { activity ->
                    (activity as? MainActivity)?.ttsManager?.speak("Estàs cridant")
                }
            }
            viewModel.stopListening()
            onShoutDetected()
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startListening()
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cal permís de micròfon")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Concedir permís")
                }
            }
        }
        return
    }

    // Animació de cercle pulsant
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Escoltant...", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(32.dp))
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = Color(0xFF185FA5).copy(alpha = 0.3f),
                    radius = 60.dp.toPx() * scale,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF185FA5),
                    radius = 8.dp.toPx()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onBack) {
                Text("Tornar")
            }
        }
    }

    // Quan es detecta crit, naveguem automàticament
    LaunchedEffect(isShouting) {
        if (isShouting) {
            viewModel.stopListening()
            onShoutDetected()
        }
    }
}