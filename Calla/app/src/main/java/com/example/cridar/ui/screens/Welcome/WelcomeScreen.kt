package com.example.cridar.ui.screens.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Shout Detector", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)) {
                Text("Començar", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)) {
                Text("Configuració", fontSize = 18.sp)
            }
        }
    }
}