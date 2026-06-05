package com.example.dinamiqapp.data

object ScaleConverter {
    // Límits de dBFS (els que fem servir a MeasurementViewModel)
    const val MIN_DBFS = -90f
    const val MAX_DBFS = 0f

    // dBFS → escala 0-100
    fun dbToScale(db: Float): Float {
        val clamped = db.coerceIn(MIN_DBFS, MAX_DBFS)
        return (clamped - MIN_DBFS) / (MAX_DBFS - MIN_DBFS) * 100f
    }

    // escala 0-100 → dBFS
    fun scaleToDb(scale: Float): Float {
        val clamped = scale.coerceIn(0f, 100f)
        return MIN_DBFS + (clamped / 100f) * (MAX_DBFS - MIN_DBFS)
    }
}