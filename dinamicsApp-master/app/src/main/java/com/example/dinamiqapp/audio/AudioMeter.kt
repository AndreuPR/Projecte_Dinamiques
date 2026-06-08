package com.example.dinamiqapp.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioFrame(val pcm: ShortArray, val db: Float)

object AudioMeter {

    private const val TAG          = "AudioMeter"
    private const val SAMPLE_RATE  = 44_100
    private const val CHANNEL_IN   = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING     = AudioFormat.ENCODING_PCM_16BIT

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun audioFrameFlow(windowMs: Long = 200): Flow<AudioFrame> = flow {
        val samplesPerWindow = (SAMPLE_RATE * windowMs / 1000).toInt().coerceAtLeast(1024)
        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        val bufSize    = maxOf(minBufSize, samplesPerWindow * 2)

        Log.d(TAG, "Creant AudioRecord: windowMs=$windowMs samplesPerWindow=$samplesPerWindow bufSize=$bufSize minBufSize=$minBufSize")

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_IN,
            ENCODING,
            bufSize
        )

        // Comprova que l'AudioRecord s'ha inicialitzat correctament
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            Log.e(TAG, "AudioRecord no s'ha inicialitzat (micròfon en ús per una altra app?)")
            throw IllegalStateException("No s'ha pogut inicialitzar el micròfon. Tanca altres apps que l'usin.")
        }

        recorder.startRecording()
        Log.d(TAG, "AudioRecord iniciat correctament")

        var frameCount = 0
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
                    val db  = if (rms > 0) 20.0 * log10(rms / 32768.0) else -90.0
                    frameCount++
                    if (frameCount <= 3 || frameCount % 50 == 0) {
                        Log.d(TAG, "Frame #$frameCount: read=$read dB=${"%.1f".format(db)}")
                    }
                    emit(AudioFrame(buffer.copyOf(read), db.toFloat().coerceIn(-90f, 0f)))
                } else {
                    Log.w(TAG, "recorder.read() retornat $read (error o buit)")
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
            Log.d(TAG, "AudioRecord aturat i alliberat. Total frames emesos: $frameCount")
        }
    }.flowOn(Dispatchers.IO)

    // Retrocompatible: els ViewModels existents que usen dbFlow() continuen funcionant
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun dbFlow(windowMs: Long = 200): Flow<Float> = audioFrameFlow(windowMs).map { it.db }
}
