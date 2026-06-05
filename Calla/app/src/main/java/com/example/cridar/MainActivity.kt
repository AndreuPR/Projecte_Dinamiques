package com.example.shoutdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.cridar.audio.ScaleConverter
import com.example.cridar.audio.TextToSpeechManager
import com.example.cridar.data.ShoutRepository
import com.example.cridar.navigation.AppNavGraph
import com.example.cridar.ui.theme.ShoutDetectorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    lateinit var ttsManager: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TextToSpeechManager(this)

        val repository = ShoutRepository(this)
        lifecycleScope.launch {
            ScaleConverter.minDb = repository.minDb.first()
            ScaleConverter.maxDb = repository.maxDb.first()
        }

        enableEdgeToEdge()
        setContent {
            ShoutDetectorTheme {  // pots crear un tema simple o reutilitzar el de DynamicsApp
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }
}