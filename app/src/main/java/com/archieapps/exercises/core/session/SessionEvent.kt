package com.archieapps.exercises.core.session

import kotlinx.serialization.Serializable

object SessionEventType {
    const val SESSION_STARTED = "session_started"
    const val SET_STARTED = "set_started"
    const val SET_FINISHED = "set_finished"
    const val REST_STARTED = "rest_started"
    const val REST_FINISHED = "rest_finished"
    const val SESSION_FINISHED = "session_finished"
}

object SessionPhaseKind {
    const val EXECUTION = "execution"
    const val REST = "rest"
    const val IDLE = "idle"
}

@Serializable
data class SessionEvent(
    val sequence: Int,
    val type: String,
    val atEpochMillis: Long,
    val exerciseOrder: Int? = null,
    val exerciseName: String? = null,
    val setNumber: Int? = null,
    val meta: Map<String, String>? = null,
) {
    val opensPhase: String?
        get() = when (type) {
            SessionEventType.SESSION_STARTED, SessionEventType.SET_FINISHED, SessionEventType.REST_FINISHED -> SessionPhaseKind.IDLE
            SessionEventType.SET_STARTED -> SessionPhaseKind.EXECUTION
            SessionEventType.REST_STARTED -> SessionPhaseKind.REST
            else -> null
        }
}
