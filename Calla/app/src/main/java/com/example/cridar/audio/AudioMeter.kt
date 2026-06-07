package com.example.cridar.audio

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

    fun dbFlow(windowMs: Long = 200): Flow<Float> = flow {
        val sampleRate = SAMPLE_RATE
        val samplesPerWindow = (sampleRate * windowMs / 1000).toInt().coerceAtLeast(1024)
        val bufSize = maxOf(
            AudioRecord.getMinBufferSize(sampleRate, CHANNEL_IN, ENCODING),
            samplesPerWindow * 2
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            CHANNEL_IN,
            ENCODING,
            bufSize
        )
        recorder.startRecording()
        try {
            val buffer = ShortArray(samplesPerWindow)
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, samplesPerWindow)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sum += s * s
                    }
                    val rms = sqrt(sum / read)
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