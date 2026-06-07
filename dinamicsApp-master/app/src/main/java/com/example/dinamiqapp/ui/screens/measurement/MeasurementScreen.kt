package com.example.dinamiqapp.ui.screens.measurement

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.dinamiqapp.data.DynamicLevel
import com.example.dinamiqapp.ui.theme.DynamicsColorPalette
import com.example.dinamiqapp.ui.theme.DynamicColors

@Composable
fun MeasurementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MeasurementViewModel = viewModel()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val precision by viewModel.precision.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.startMeasuring()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startMeasuring()
    }

    // Colors segons la dinàmica
    val dynamicColors: DynamicColors = when (currentLevel) {
        DynamicLevel.PP -> DynamicsColorPalette.PP_LIGHT
        DynamicLevel.P  -> DynamicsColorPalette.P_LIGHT
        DynamicLevel.MF -> DynamicsColorPalette.MF_LIGHT
        DynamicLevel.F  -> DynamicsColorPalette.F_LIGHT
        DynamicLevel.FF -> DynamicsColorPalette.FF_LIGHT
        null -> DynamicColors(Color.Transparent, MaterialTheme.colorScheme.onSurface, Color.Gray)
    }

    // Opacitat del fons: augmenta amb la precisió (0.4 → 1.0)
    val backgroundAlpha = 0.4f + precision * 0.6f

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cal el permís de micròfon per mesurar el so.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Concedir permís")
                }
            }
        }
    } else {
        androidx.compose.ui.platform.LocalView.current.keepScreenOn = true
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicColors.background.copy(alpha = backgroundAlpha))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentLevel?.symbol ?: "?",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = dynamicColors.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentLevel?.fullName ?: "Sense dinàmica",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = dynamicColors.onBackground.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(80.dp))
                TextButton(onClick = onBack) {
                    Text("← Tornar", color = dynamicColors.onBackground)
                }
            }
        }
    }
}