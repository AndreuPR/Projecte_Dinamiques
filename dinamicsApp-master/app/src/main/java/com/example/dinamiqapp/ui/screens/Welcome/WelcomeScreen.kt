package com.example.dinamiqapp.ui.screens.welcome

import androidx.compose.foundation.background
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
    onNavigateToMeasurement: () -> Unit,
    onNavigateToMeter: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECEFF1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Dinàmiques",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A18)
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Botó 1: Mesura de dinàmiques (sense números)
            Button(
                onClick = onNavigateToMeasurement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF185FA5)
                )
            ) {
                Text("Començar a mesurar", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botó 2: Medidor amb cercle i intensitat
            Button(
                onClick = onNavigateToMeter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF185FA5)
                )
            ) {
                Text("Medidor", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botó 3: Configuració (menú)
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Configuració", fontSize = 18.sp)
            }
        }
    }
}