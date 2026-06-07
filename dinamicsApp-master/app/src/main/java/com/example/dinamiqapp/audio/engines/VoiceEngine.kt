package com.example.dinamiqapp.audio.engines

/**
 * Tipus de motor de reconeixement de veu/instrument.
 *
 * MFCC        — Template matching clàssic. Ràpid, sense arxius externs.
 *               Entrena per registres (greu/mig/agut) per a millors resultats.
 *
 * TENSORFLOW  — Xarxa neuronal pre-entrenada (YAMNet). Molt més precís,
 *               especialment per a timbre i registres aguts. Requereix
 *               l'arxiu `yamnet.tflite` a assets/. Entrena durant una
 *               sessió completa (30-60s) per a resultats òptims.
 */
enum class EngineType { MFCC, TENSORFLOW }

/**
 * Interfície comuna per als dos motors de reconeixement.
 * FilteredAudioSource usa aquesta interfície; no li importa quin motor hi ha a dins.
 */
interface VoiceEngine {
    val type: EngineType

    /** Extreu un vector de característiques d'un frame PCM (44100 Hz, ShortArray) */
    fun extract(pcm: ShortArray): FloatArray

    /** Similitud cosinus entre dos vectors de característiques. Rang [0, 1] */
    fun similarity(a: FloatArray, b: FloatArray): Float
}
