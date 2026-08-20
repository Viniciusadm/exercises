package com.archieapps.exercises.ui.feature.anatomy

import com.archieapps.exercises.core.net.dto.MuscleVolumeDto
import com.archieapps.exercises.ui.component.ShareRow
import com.archieapps.exercises.ui.design.muscleColorFor
import kotlin.math.ceil
import com.archieapps.exercises.core.net.dto.ExerciseMusclesDto
import com.archieapps.exercises.ui.component.MuscleActivation
import com.archieapps.exercises.ui.component.MuscleRegionValue

const val STATUS_UNTRAINED = "untrained"
const val STATUS_STABILIZER_ONLY = "stabilizer_only"
const val STATUS_UNDERTRAINED = "undertrained"
const val STATUS_BELOW_BASELINE = "below_baseline"
const val STATUS_BALANCED = "balanced"
const val STATUS_EMPHASIZED = "emphasized"

private const val MIN_INTENSITY = 0.28f
private const val PERCENTILE = 0.9

fun setsPerWeekReference(muscles: List<MuscleVolumeDto>, percentile: Double = PERCENTILE): Double {
    val values = muscles.map { it.setsPerWeek }.filter { it > 0.0 }.sorted()
    if (values.isEmpty()) return 1.0

    val index = (ceil(percentile * values.size).toInt() - 1).coerceIn(0, values.size - 1)

    return values[index].takeIf { it > 0.0 } ?: 1.0
}

fun muscleIntensities(
    muscles: List<MuscleVolumeDto>,
    percentile: Double = PERCENTILE,
): Map<String, Float> {
    val reference = setsPerWeekReference(muscles, percentile)

    return muscles.associate { muscle ->
        muscle.slug to when {
            muscle.setsPerWeek <= 0.0 -> 0f
            else -> (muscle.setsPerWeek / reference).toFloat().coerceIn(MIN_INTENSITY, 1f)
        }
    }
}

fun activationRegions(muscles: ExerciseMusclesDto): List<MuscleRegionValue> {
    val strongest = linkedMapOf<String, Pair<MuscleActivation, String>>()

    listOf(
        MuscleActivation.STABILIZER to muscles.stabilizer,
        MuscleActivation.SECONDARY to muscles.secondary,
        MuscleActivation.PRIMARY to muscles.primary,
    ).forEach { (activation, list) ->
        list.forEach { muscle ->
            muscle.svgRegions.forEach { region -> strongest[region] = activation to muscle.name }
        }
    }

    return strongest.map { (region, entry) ->
        MuscleRegionValue(
            regionId = region,
            muscleName = entry.second,
            intensity = when (entry.first) {
                MuscleActivation.PRIMARY -> 1f
                MuscleActivation.SECONDARY -> 0.5f
                MuscleActivation.STABILIZER -> 0.2f
            },
            activation = entry.first,
        )
    }
}

fun topMuscles(muscles: List<MuscleVolumeDto>, limit: Int = 8): List<MuscleVolumeDto> =
    muscles.sortedWith(compareByDescending<MuscleVolumeDto> { it.setsPerWeek }.thenBy { it.name })
        .take(limit)

fun shareRows(muscles: List<MuscleVolumeDto>, limit: Int = 8): List<ShareRow> {
    val top = topMuscles(muscles, limit)
    val reference = top.maxOfOrNull { it.setsPerWeek }?.takeIf { it > 0.0 } ?: 1.0

    return top.map { muscle ->
        ShareRow(
            label = muscle.name,
            value = "${trimZero(muscle.setsPerWeek)} sér/sem",
            share = (muscle.setsPerWeek / reference).coerceIn(0.0, 1.0),
            color = muscleColorFor(muscle.slug),
        )
    }
}

fun trimZero(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0

    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString().replace('.', ',')
}

fun statusLabel(status: String): String = when (status) {
    STATUS_UNTRAINED -> "sem estímulo"
    STATUS_STABILIZER_ONLY -> "só estabilizador"
    STATUS_UNDERTRAINED -> "muito abaixo"
    STATUS_BELOW_BASELINE -> "abaixo da média"
    STATUS_BALANCED -> "equilibrado"
    STATUS_EMPHASIZED -> "em ênfase"
    else -> status
}
