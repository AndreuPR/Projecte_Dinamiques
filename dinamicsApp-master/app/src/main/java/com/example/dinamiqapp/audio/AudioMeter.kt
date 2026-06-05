package com.example.dinamiqapp.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Continuously reads from the microphone and emits dB RMS values.
 * Collect this flow inside a coroutine scope tied to the screen lifecycle.
 * The flow completes automatically when the coroutine is cancelled.
 */
object AudioMeter {

    private const val SAMPLE_RATE  = 44_100
    private const val CHANNEL_IN   = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING     = AudioFormat.ENCODING_PCM_16BIT
    // ~100ms window at 44100 Hz
    private const val READ_SAMPLES = 4_096
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)

    fun dbFlow(): Flow<Float> = flow  {
        val bufSize = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING),
            READ_SAMPLES * 2
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_IN,
            ENCODING,
            bufSize
        )
        recorder.startRecording()
        try {
            val buffer = ShortArray(READ_SAMPLES)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, READ_SAMPLES)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sum += s * s
                    }
                    val rms = sqrt(sum / read)
                    // Normalise against 16-bit max (32768), convert to dBFS
                    val db = if (rms > 0) 20.0 * log10(rms / 32768.0) else -90.0
                    emit(db.toFloat().coerceIn(-90f, 0f))
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}