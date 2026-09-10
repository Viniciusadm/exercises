package com.archieapps.exercises.core.session

object DistanceLadder {
    const val MIN_KM = 0.0
    const val MAX_KM = 999.999
    const val STEP_KM = 0.1

    fun up(current: Double?): Double = clamp((current ?: MIN_KM) + STEP_KM)

    fun down(current: Double?): Double = clamp((current ?: MIN_KM) - STEP_KM)

    fun clamp(value: Double): Double {
        if (!value.isFinite()) return MIN_KM

        return Math.round(value.coerceIn(MIN_KM, MAX_KM) * 1000.0) / 1000.0
    }
}
