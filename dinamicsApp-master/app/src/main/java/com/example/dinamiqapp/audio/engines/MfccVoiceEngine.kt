package com.example.dinamiqapp.audio.engines

import com.example.dinamiqapp.audio.MfccExtractor

/**
 * Motor MFCC: 13 coeficients + similitud cosinus.
 * Template matching clàssic — ràpid i sense dependències externes.
 * Per a millors resultats, entrena per registres (greu / mig / agut).
 */
object MfccVoiceEngine : VoiceEngine {
    override val type = EngineType.MFCC

    override fun extract(pcm: ShortArray): FloatArray =
        MfccExtractor.extract(pcm)

    override fun similarity(a: FloatArray, b: FloatArray): Float =
        MfccExtractor.cosineSimilarity(a, b)
}
