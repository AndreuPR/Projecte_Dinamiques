package com.example.cridar.ui.screens.shouting


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShoutingScreen(
    onApologize: () -> Unit,       // "Perdò" → torna al menú principal
    onRetry: () -> Unit             // "Tornar a intentar" → torna a Listening
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Estàs cridant!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onApologize, modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)) {
                Text("Perdó")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)) {
                Text("Tornar a intentar")
            }
        }
    }
}