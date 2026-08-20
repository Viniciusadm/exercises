package com.archieapps.exercises.core.rest

import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado do descanso compartilhado entre o serviço (que segura o processo vivo em background)
 * e a UI. A contagem é derivada de um instante de término, não de um contador decrementado,
 * então atrasos de agendamento não desalinham o cronômetro.
 */
object RestTimerBus {
    data class Snapshot(
        val exerciseName: String,
        val totalSeconds: Int,
        val endsAtMillis: Long,
    ) {
        fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int =
            ceil((endsAtMillis - nowMillis) / 1000.0).toInt().coerceAtLeast(0)

        fun isOver(nowMillis: Long = System.currentTimeMillis()): Boolean = remainingSeconds(nowMillis) <= 0
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

    fun add(seconds: Int) = _state.update { snapshot ->
        snapshot?.copy(
            totalSeconds = snapshot.totalSeconds + seconds,
            endsAtMillis = snapshot.endsAtMillis + seconds * 1_000L,
        )
    }

    fun clear() {
        _state.value = null
    }
}
