package com.archieapps.exercises.core.rest

import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object RestTimerBus {
    data class Snapshot(
        val exerciseName: String,
        val totalSeconds: Int,
        val endsAtMillis: Long,
        val countUp: Boolean = false,
        val startedAtMillis: Long = 0L,
    ) {
        fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int =
            ceil((endsAtMillis - nowMillis) / 1000.0).toInt().coerceAtLeast(0)

        fun elapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Int =
            max(0L, (nowMillis - startedAtMillis) / 1000L).toInt()

        fun isOver(nowMillis: Long = System.currentTimeMillis()): Boolean =
            !countUp && remainingSeconds(nowMillis) <= 0
    }

    private val _state = MutableStateFlow<Snapshot?>(null)
    val state: StateFlow<Snapshot?> = _state

    val current: Snapshot? get() = _state.value

    fun start(exerciseName: String, seconds: Int) {
        _state.value = Snapshot(
            exerciseName = exerciseName,
            totalSeconds = seconds.coerceAtLeast(1),
            endsAtMillis = System.currentTimeMillis() + seconds.coerceAtLeast(1) * 1_000L,
        )
    }

    fun startExecution(exerciseName: String, startedAtMillis: Long) {
        _state.value = Snapshot(
            exerciseName = exerciseName,
            totalSeconds = 0,
            endsAtMillis = 0L,
            countUp = true,
            startedAtMillis = startedAtMillis,
        )
    }

    fun add(seconds: Int) = _state.update { snapshot ->
        if (snapshot == null || snapshot.countUp) {
            snapshot
        } else {
            snapshot.copy(
                totalSeconds = snapshot.totalSeconds + seconds,
                endsAtMillis = snapshot.endsAtMillis + seconds * 1_000L,
            )
        }
    }

    fun clear() {
        _state.value = null
    }
}
