package com.example.dinamiqapp.data

object ScaleConverter {
    // Rang global de l'app (configurat a AppSettings)
    var appMinDb = -70f
    var appMaxDb = 0f

    // Rang actiu del perfil (s'actualitza quan es carrega un perfil calibrat)
    // Si és null, es fa servir el rang global
    var profileMinDb: Float? = null
    var profileMaxDb: Float? = null

    private val minDb get() = profileMinDb ?: appMinDb
    private val maxDb get() = profileMaxDb ?: appMaxDb

    fun dbToScale(db: Float): Float {
        val clamped = db.coerceIn(minDb, maxDb)
        return (clamped - minDb) / (maxDb - minDb) * 100f
    }

    fun scaleToDb(scale: Float): Float {
        val clamped = scale.coerceIn(0f, 100f)
        return minDb + (clamped / 100f) * (maxDb - minDb)
    }

    // Estira l'escala perquè cobreixi exactament el rang del perfil calibrat
    fun updateFromRanges(ranges: Map<DynamicLevel, DynamicRange>) {
        val allMins = ranges.values.map { it.min }
        val allMaxs = ranges.values.map { it.max }
        val rangeMin = allMins.min()
        val rangeMax = allMaxs.max()
        // Afegim un petit marge (10% a cada costat) perquè els extrems no quedin tallats
        val margin = (rangeMax - rangeMin) * 0.05f
        profileMinDb = (rangeMin - margin).coerceAtLeast(appMinDb)
        profileMaxDb = (rangeMax + margin).coerceAtMost(appMaxDb)
    }

    fun clearProfileRange() {
        profileMinDb = null
        profileMaxDb = null
    }
}