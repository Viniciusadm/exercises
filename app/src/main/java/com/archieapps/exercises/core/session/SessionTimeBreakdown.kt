package com.archieapps.exercises.core.session

import kotlin.math.max
import kotlin.math.roundToInt

data class SessionTimeBreakdown(
    val totalSeconds: Long = 0L,
    val executionSeconds: Long = 0L,
    val restSeconds: Long = 0L,
    val idleSeconds: Long = 0L,
    val setsCount: Int = 0,
    val restPeriodsCount: Int = 0,
) {
    val executionShare: Float get() = share(executionSeconds)

    val restShare: Float get() = share(restSeconds)

    val idleShare: Float get() = share(idleSeconds)

    val avgSetSeconds: Int? get() = if (setsCount > 0) (executionSeconds.toDouble() / setsCount).roundToInt() else null

    val avgRestSeconds: Int?
        get() = if (restPeriodsCount > 0) (restSeconds.toDouble() / restPeriodsCount).roundToInt() else null

    private fun share(part: Long): Float = if (totalSeconds > 0) part.toFloat() / totalSeconds else 0f

    companion object {
        fun of(session: ActiveSession, nowMillis: Long): SessionTimeBreakdown {
            var execution = 0L
            var rest = 0L
            var idle = 0L
            var sets = 0
            var periods = 0

            session.events.forEachIndexed { index, event ->
                if (event.type == SessionEventType.SET_FINISHED) sets++
                if (event.type == SessionEventType.REST_STARTED) periods++

                val phase = event.opensPhase ?: return@forEachIndexed
                val endsAt = session.events.getOrNull(index + 1)?.atEpochMillis ?: nowMillis
                val seconds = max(0L, (endsAt - event.atEpochMillis) / 1000L)

                when (phase) {
                    SessionPhaseKind.EXECUTION -> execution += seconds
                    SessionPhaseKind.REST -> rest += seconds
                    else -> idle += seconds
                }
            }

            return SessionTimeBreakdown(
                totalSeconds = execution + rest + idle,
                executionSeconds = execution,
                restSeconds = rest,
                idleSeconds = idle,
                setsCount = sets,
                restPeriodsCount = periods,
            )
        }
    }
}

data class LocalTimelineSegment(
    val sequence: Int,
    val phase: String,
    val label: String,
    val atEpochMillis: Long,
    val durationSeconds: Long,
    val exerciseName: String? = null,
    val setNumber: Int? = null,
)

object SessionTimelineLocal {
    fun segments(session: ActiveSession, nowMillis: Long): List<LocalTimelineSegment> =
        session.events.mapIndexedNotNull { index, event ->
            val phase = event.opensPhase ?: return@mapIndexedNotNull null
            val endsAt = session.events.getOrNull(index + 1)?.atEpochMillis ?: nowMillis
            val seconds = max(0L, (endsAt - event.atEpochMillis) / 1000L)

            if (seconds == 0L) {
                null
            } else {
                LocalTimelineSegment(
                    sequence = event.sequence,
                    phase = phase,
                    label = label(phase, event),
                    atEpochMillis = event.atEpochMillis,
                    durationSeconds = seconds,
                    exerciseName = event.exerciseName,
                    setNumber = event.setNumber,
                )
            }
        }

    private fun label(phase: String, event: SessionEvent): String = when (phase) {
        SessionPhaseKind.EXECUTION -> listOfNotNull(
            event.setNumber?.let { "Série $it" } ?: "Série",
            event.exerciseName,
        ).joinToString(" · ")

        SessionPhaseKind.REST -> "Descanso"
        else -> "Ocioso"
    }
}
