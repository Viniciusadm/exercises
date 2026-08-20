package com.archieapps.exercises.core.session

import kotlin.math.ceil
import kotlin.math.floor

object WeightLadder {
    const val MIN_KG = 0.0
    const val MAX_KG = 999.0
    const val DEFAULT_STEP_KG = 1.0

    private const val EPSILON = 1e-6

    fun up(current: Double?, options: List<Double>): Double = step(current, options, ascending = true)

    fun down(current: Double?, options: List<Double>): Double = step(current, options, ascending = false)

    fun sanitize(options: List<Double>): List<Double> = options
        .filter { it.isFinite() && it >= MIN_KG && it <= MAX_KG }
        .map { round(it) }
        .distinct()
        .sorted()

    private fun step(current: Double?, options: List<Double>, ascending: Boolean): Double {
        val from = (current ?: MIN_KG).coerceIn(MIN_KG, MAX_KG)
        val ladder = sanitize(options)

        val next = if (ladder.isEmpty()) {
            if (ascending) floor(from + EPSILON) + DEFAULT_STEP_KG else ceil(from - EPSILON) - DEFAULT_STEP_KG
        } else {
            val neighbour = if (ascending) {
                ladder.firstOrNull { it > from + EPSILON }
            } else {
                ladder.lastOrNull { it < from - EPSILON }
            }

            neighbour ?: from
        }

        return round(next.coerceIn(MIN_KG, MAX_KG))
    }

    private fun round(value: Double): Double = Math.round(value * 100.0) / 100.0
}
