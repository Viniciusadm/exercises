package com.archieapps.exercises.core.format

object Pace {
    fun speedKmh(km: Double?, seconds: Int?): Double? {
        if (km == null || seconds == null || km <= 0.0 || seconds <= 0) return null

        return Math.round(km / (seconds / 3600.0) * 100) / 100.0
    }

    fun secondsPerKm(km: Double?, seconds: Int?): Int? {
        if (km == null || seconds == null || km <= 0.0 || seconds <= 0) return null

        return Math.round(seconds / km).toInt()
    }
}
