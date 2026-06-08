package com.example.dinamiqapp.audio.engines

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Motor TF Lite basat en YAMNet (Google AudioSet).
 *
 * YAMNet és una xarxa neuronal pre-entrenada amb milions de clips d'àudio.
 * Genera embeddings de 1024 dimensions que capturen el timbre de forma
 * robusta: funciona igual a les notes greus, mig i agudes (és pitch-invariant).
 *
 * CONFIGURACIÓ (una sola vegada):
 * 1. Descarrega el model:
 *    https://storage.googleapis.com/download.tensorflow.org/models/tflite/
 *    task_library/audio_classification/android/
 *    lite-model_yamnet_classification_tflite_1.tflite
 * 2. Reanomena'l a: yamnet.tflite
 * 3. Col·loca'l a: app/src/main/assets/yamnet.tflite
 *
 * FUNCIONAMENT:
 * - Input YAMNet:  float32[15600] — 0.975s d'àudio a 16kHz, normalitzat [-1, 1]
 * - Output idx 1:  float32[3, 1024] — embeddings de 3 sub-finestres
 * - Utilitza un buffer circular: els primers ~1s d'àudio fan d'"escalfament"
 *
 * ENTRENAMENT:
 * - Grava 30-60s tocant/parlant normalment
 * - El centroid és la mitjana de tots els embeddings → vector de 1024 floats
 * - Guardat al DataStore igual que el centroid MFCC (és un FloatArray més gran)
 */
class TensorFlowVoiceEngine(context: Context) : VoiceEngine {

    override val type = EngineType.TENSORFLOW

    companion object {
        private const val TAG             = "TFVoiceEngine"
        const val MODEL_FILE              = "yamnet.tflite"
        private const val SAMPLE_RATE_IN  = 44100
        private const val SAMPLE_RATE_OUT = 16000
        const val YAMNET_INPUT_SIZE       = 15600   // 0.975s a 16kHz
        const val EMBEDDING_SIZE          = 1024    // fallback; el valor real es detecta en runtime
    }

    // Mida real de la sortida (521 per classification-only, 1024 per embedding model)
    private var detectedOutputSize: Int = EMBEDDING_SIZE

    private val appContext = context.applicationContext

    // ── Disponibilitat del model ─────────────────────────────────────────────

    val isAvailable: Boolean get() = try {
        appContext.assets.open(MODEL_FILE).close()
        true
    } catch (_: IOException) { false }

    private val interpreter: Interpreter? by lazy {
        if (!isAvailable) return@lazy null
        try {
            val afd    = appContext.assets.openFd(MODEL_FILE)
            val buffer = FileInputStream(afd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            Interpreter(buffer).also { interp ->
                // Log shapes per a debug
                val outCount = interp.outputTensorCount
                Log.d(TAG, "YAMNet carregat. Sortides: $outCount")
                for (i in 0 until outCount) {
                    Log.d(TAG, "  Output $i: ${interp.getOutputTensor(i).shape().contentToString()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error carregant YAMNet: ${e.message}")
            null
        }
    }

    // ── Buffer circular 16kHz ────────────────────────────────────────────────

    private val rollingBuffer = FloatArray(YAMNET_INPUT_SIZE)
    private var totalSamplesAdded = 0L

    /**
     * Extreu un embedding YAMNet de 1024 dims del frame PCM donat.
     * Retorna un vector de zeros mentre el buffer s'omple (~1s inicial).
     */
    override fun extract(pcm: ShortArray): FloatArray {
        val resampled = resample(pcm)
        // Afegeix mostres al buffer circular
        for (s in resampled) {
            rollingBuffer[(totalSamplesAdded % YAMNET_INPUT_SIZE).toInt()] = s
            totalSamplesAdded++
        }
        // Esperem que el buffer estigui ple
        if (totalSamplesAdded < YAMNET_INPUT_SIZE) return FloatArray(detectedOutputSize)
        return runInference() ?: FloatArray(detectedOutputSize)
    }

    /** Torna a zero el buffer (usar quan es canvia de perfil o s'inicia entrenament) */
    fun reset() {
        totalSamplesAdded = 0L
        rollingBuffer.fill(0f)
    }

    // ── Similitud cosinus (1024 dims) ────────────────────────────────────────

    override fun similarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) { dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i] }
        return if (normA == 0f || normB == 0f) 0f
               else dot / (sqrt(normA) * sqrt(normB))
    }

    // ── Privat ───────────────────────────────────────────────────────────────

    // Índex de sortida dels embeddings: es detecta automàticament al primer ús
    private var embeddingOutputIndex: Int = -1

    private fun runInference(): FloatArray? {
        val interp = interpreter ?: return null

        // Ordena el buffer circular: del més antic al més nou
        val input    = FloatArray(YAMNET_INPUT_SIZE)
        val startPos = (totalSamplesAdded % YAMNET_INPUT_SIZE).toInt()
        for (i in 0 until YAMNET_INPUT_SIZE) {
            input[i] = rollingBuffer[(startPos + i) % YAMNET_INPUT_SIZE]
        }

        return try {
            // Detecta l'índex de sortida dels embeddings si encara no ho hem fet
            if (embeddingOutputIndex < 0) {
                embeddingOutputIndex = findEmbeddingOutputIndex(interp)
                Log.d(TAG, "Índex de sortida dels embeddings: $embeddingOutputIndex")
            }

            if (embeddingOutputIndex < 0) {
                Log.e(TAG, "No s'ha trobat cap sortida compatible amb embeddings. Shapes disponibles:")
                for (i in 0 until interp.outputTensorCount) {
                    Log.e(TAG, "  Output $i: ${interp.getOutputTensor(i).shape().contentToString()} dtype=${interp.getOutputTensor(i).dataType()}")
                }
                return null
            }

            val embShape = interp.getOutputTensor(embeddingOutputIndex).shape()
            Log.v(TAG, "Inferència YAMNet. Input range=[${input.min()}, ${input.max()}] EmbShape=${embShape.contentToString()}")

            // Alloca la sortida adaptant-se al shape real del model
            val embedding = when (embShape.size) {
                1 -> {
                    // Shape [dims] — un sol frame
                    val out = FloatArray(embShape[0])
                    interp.runForMultipleInputsOutputs(arrayOf(input), mapOf(embeddingOutputIndex to out))
                    out
                }
                2 -> {
                    // Shape [numFrames, dims] — múltiples frames → promig
                    val numFrames = embShape[0]; val dims = embShape[1]
                    val out = Array(numFrames) { FloatArray(dims) }
                    interp.runForMultipleInputsOutputs(arrayOf(input), mapOf(embeddingOutputIndex to out))
                    FloatArray(dims) { i -> out.sumOf { it[i].toDouble() }.toFloat() / numFrames }
                }
                else -> {
                    Log.e(TAG, "Shape de sortida no suportada: ${embShape.contentToString()}")
                    return null
                }
            }

            Log.v(TAG, "Embedding OK. Dims=${embedding.size} range=[${embedding.min()}, ${embedding.max()}]")
            embedding

        } catch (e: Exception) {
            Log.e(TAG, "Error en inferència YAMNet: ${e.message}", e)
            null
        }
    }

    /**
     * Troba l'índex de sortida més útil per a similitud de timbre.
     * Estratègia: agafa la sortida float32 amb més dimensions.
     * - Models embedding (recomanats): output [3,1024] o [1024] → 1024 dims
     * - Models classification-only: output [1,521] → 521 dims (menys bo però funcional)
     */
    private fun findEmbeddingOutputIndex(interp: Interpreter): Int {
        var bestIdx  = -1
        var bestSize = 0
        for (i in 0 until interp.outputTensorCount) {
            val shape = interp.getOutputTensor(i).shape()
            val size  = shape.last()
            Log.d(TAG, "  Output $i: ${shape.contentToString()} dtype=${interp.getOutputTensor(i).dataType()} lastDim=$size")
            if (size > bestSize) {
                bestSize = size
                bestIdx  = i
            }
        }
        if (bestIdx >= 0) {
            detectedOutputSize = bestSize
            val quality = when {
                bestSize >= 512 -> "bona (embeddings)"
                bestSize >= 100 -> "acceptable (scores de classificació)"
                else            -> "pobra"
            }
            Log.d(TAG, "Sortida seleccionada: índex=$bestIdx dims=$bestSize qualitat=$quality")
            if (bestSize == 521) {
                Log.i(TAG, "⚠ Model classification-only (521 scores). Funciona, però per millors resultats " +
                           "descarrega la versió amb embeddings de https://www.kaggle.com/models/google/yamnet")
            }
        }
        return bestIdx
    }

    /** Interpolació lineal: 44100 Hz → 16000 Hz, normalitzat a [-1, 1] */
    private fun resample(pcm: ShortArray): FloatArray {
        val ratio      = SAMPLE_RATE_IN.toDouble() / SAMPLE_RATE_OUT
        val outputSize = (pcm.size / ratio).toInt()
        return FloatArray(outputSize) { i ->
            val srcIdx = i * ratio
            val lo     = srcIdx.toInt().coerceIn(0, pcm.size - 1)
            val hi     = (lo + 1).coerceIn(0, pcm.size - 1)
            val frac   = (srcIdx - lo).toFloat()
            (pcm[lo] * (1f - frac) + pcm[hi] * frac) / 32768f
        }
    }
}
