package com.example.cridar.audio


object ScaleConverter {
    var minDb = -70f
    var maxDb = 0f

    fun dbToScale(db: Float): Float {
        val clamped = db.coerceIn(minDb, maxDb)
        return (clamped - minDb) / (maxDb - minDb) * 100f
    }

    fun scaleToDb(scale: Float): Float {
        val clamped = scale.coerceIn(0f, 100f)
        return minDb + (clamped / 100f) * (maxDb - minDb)
    }
}