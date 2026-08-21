package com.archieapps.exercises.core.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SessionPhase {
    @Serializable
    @SerialName("idle")
    object Idle : SessionPhase

    @Serializable
    @SerialName("executing")
    data class Executing(
        val exerciseIndex: Int,
        val setIndex: Int,
        val startedAtMillis: Long,
    ) : SessionPhase

    @Serializable
    @SerialName("resting")
    data class Resting(
        val exerciseIndex: Int,
        val setIndex: Int,
        val startedAtMillis: Long,
        val endsAtMillis: Long,
    ) : SessionPhase
}
