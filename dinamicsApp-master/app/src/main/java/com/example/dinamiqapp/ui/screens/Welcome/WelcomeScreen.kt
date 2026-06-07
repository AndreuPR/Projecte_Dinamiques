package com.example.dinamiqapp.ui.screens.welcome

import android.app.Application
import androidx.compose.foundation.background
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinamiqapp.data.VoiceProfile
import com.example.dinamiqapp.data.VoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// ViewModel lleuger per a la pantalla de benvinguda
class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VoiceRepository(application)

    data class VoiceState(val activeProfile: VoiceProfile?, val hasAny: Boolean)

    val voiceState: StateFlow<VoiceState> = repo.allProfiles.combine(repo.activeVoiceId) { profiles, activeId ->
        val active = profiles.firstOrNull { it.id == activeId }
        VoiceState(active, profiles.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), VoiceState(null, false))
}

@Composable
fun WelcomeScreen(
    onNavigateToMeasurement: () -> Unit,
    onNavigateToMeter: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVoices: () -> Unit
) {
    val vm: WelcomeViewModel = viewModel()
    val voiceState by vm.voiceState.collectAsState()

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

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de veu activa
            VoiceSelector(
                activeProfile = voiceState.activeProfile,
                hasAny        = voiceState.hasAny,
                onClick       = onNavigateToVoices
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToMeasurement,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF185FA5))
            ) {
                Text("Començar a mesurar", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToMeter,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF185FA5))
            ) {
                Text("Medidor", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Configuració", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun VoiceSelector(
    activeProfile: VoiceProfile?,
    hasAny: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (activeProfile != null) Color(0xFF7B1FA2) else Color(0xFFB0BEC5)
    val textColor      = Color.White

    Surface(
        shape  = RoundedCornerShape(24.dp),
        color  = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (activeProfile != null) "Instrument / veu activa" else "Cap instrument seleccionat",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.75f)
                )
                Text(
                    text = activeProfile?.name ?: "Toca aquí per configurar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text("›", fontSize = 24.sp, color = textColor)
        }
    }
}
