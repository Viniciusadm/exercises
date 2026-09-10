package com.archieapps.exercises.core.rest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FinishSetBus {
    private val _requestedAt = MutableStateFlow<Long?>(null)
    val requestedAt: StateFlow<Long?> = _requestedAt

    fun request() {
        _requestedAt.value = System.currentTimeMillis()
    }

    fun consume() {
        _requestedAt.value = null
    }
}
