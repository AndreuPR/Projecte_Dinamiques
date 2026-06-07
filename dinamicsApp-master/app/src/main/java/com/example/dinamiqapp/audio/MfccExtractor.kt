package com.example.dinamiqapp.audio

import kotlin.math.*

/**
 * Extreu 13 coeficients MFCC d'un frame de PCM.
 * Implementació pròpia: Hamming window → FFT → Mel filterbank → log → DCT.
 * No requereix cap biblioteca externa.
 */
object MfccExtractor {

    private const val SAMPLE_RATE   = 44_100
    private const val FRAME_SIZE    = 1024    // ~23ms a 44100 Hz (ha de ser potència de 2)
    private const val NUM_MEL       = 26
    const val NUM_COEFFICIENTS      = 13

    // Precalculem la finestra de Hamming i el filterbank (cost zero en temps real)
    private val hammingWindow = FloatArray(FRAME_SIZE) { i ->
        (0.54f - 0.46f * cos(2.0 * PI * i / (FRAME_SIZE - 1))).toFloat()
    }
    private val melFilterbank: Array<FloatArray> by lazy { buildMelFilterbank() }

    /**
     * Retorna un array de NUM_COEFFICIENTS floats que representen el timbre del frame.
     * Si el frame és massa curt, s'omple amb zeros.
     */
    fun extract(frame: ShortArray): FloatArray {
        val n = FRAME_SIZE
        val re = FloatArray(n)
        val im = FloatArray(n)

        // 1. Normalitza PCM i aplica finestra de Hamming
        val len = minOf(frame.size, n)
        for (i in 0 until len) {
            re[i] = (frame[i] / 32768f) * hammingWindow[i]
        }

        // 2. FFT in-place (Cooley-Tukey)
        fft(re, im)

        // 3. Espectre de potència (meitat positiva)
        val halfN = n / 2 + 1
        val power = FloatArray(halfN) { i -> re[i] * re[i] + im[i] * im[i] }

        // 4. Aplica Mel filterbank i pren el log
        val logEnergy = FloatArray(NUM_MEL) { m ->
            val energy = melFilterbank[m].indices.sumOf { k ->
                (melFilterbank[m][k] * power[k]).toDouble()
            }.toFloat()
            ln(energy.coerceAtLeast(1e-10f))
        }

        // 5. DCT tipus II → MFCC
        return FloatArray(NUM_COEFFICIENTS) { n2 ->
            var sum = 0f
            for (m in 0 until NUM_MEL) {
                sum += logEnergy[m] * cos(PI * n2 * (2 * m + 1) / (2.0 * NUM_MEL)).toFloat()
            }
            sum
        }
    }

    /**
     * Similitud del cosinus entre dos vectors MFCC. Retorna 0..1 (1 = idèntics).
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot   += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom < 1e-10f) 0f else (dot / denom).coerceIn(0f, 1f)
    }

    // ── Privat ──────────────────────────────────────────────────────────────

    private fun hzToMel(hz: Float) = 2595f * log10(1f + hz / 700f)
    private fun melToHz(mel: Float) = 700f * (10f.pow(mel / 2595f) - 1f)

    private fun buildMelFilterbank(): Array<FloatArray> {
        val halfN    = FRAME_SIZE / 2 + 1
        val melMin   = hzToMel(0f)
        val melMax   = hzToMel(SAMPLE_RATE / 2f)
        val melPts   = FloatArray(NUM_MEL + 2) { i ->
            melMin + i * (melMax - melMin) / (NUM_MEL + 1)
        }
        val hzPts    = FloatArray(NUM_MEL + 2) { melToHz(melPts[it]) }
        val binPts   = IntArray(NUM_MEL + 2) { i ->
            ((FRAME_SIZE + 1) * hzPts[i] / SAMPLE_RATE).toInt().coerceIn(0, halfN - 1)
        }

        return Array(NUM_MEL) { m ->
            FloatArray(halfN) { k ->
                when {
                    k < binPts[m]     -> 0f
                    k <= binPts[m+1]  -> if (binPts[m+1] == binPts[m]) 1f
                                         else (k - binPts[m]).toFloat() / (binPts[m+1] - binPts[m])
                    k <= binPts[m+2]  -> if (binPts[m+2] == binPts[m+1]) 0f
                                         else (binPts[m+2] - k).toFloat() / (binPts[m+2] - binPts[m+1])
                    else              -> 0f
                }
            }
        }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // Bit-reversal
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }
        // Butterfly Cooley-Tukey
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var pos = 0
            while (pos < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[pos + k]; val uIm = im[pos + k]
                    val half = pos + k + len / 2
                    val vRe = re[half] * curRe - im[half] * curIm
                    val vIm = re[half] * curIm + im[half] * curRe
                    re[pos + k] = uRe + vRe; im[pos + k] = uIm + vIm
                    re[half] = uRe - vRe;    im[half] = uIm - vIm
                    val newCurRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = newCurRe
                }
                pos += len
            }
            len = len shl 1
        }
    }
}
