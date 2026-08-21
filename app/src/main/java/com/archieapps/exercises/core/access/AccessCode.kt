package com.archieapps.exercises.core.access

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccessCode {
    const val HEADER = "code"

    const val LENGTH = 8

    private val _present = MutableStateFlow(false)
    val present: StateFlow<Boolean> = _present

    private val _requestedAt = MutableStateFlow<Long?>(null)
    val requestedAt: StateFlow<Long?> = _requestedAt

    @Volatile
    private var code: String? = null

    fun header(): String? = code

    fun remember(value: String) {
        code = value.takeIf { it.length == LENGTH }
        _present.value = code != null
    }

    fun forget() {
        code = null
        _present.value = false
    }

    fun request() {
        _requestedAt.value = System.currentTimeMillis()
    }

    fun consume() {
        _requestedAt.value = null
    }

    fun pairs(): List<Pair<Int, Int>> {
        val digits = (0..9).shuffled()

        return (0 until 10 step 2).map { digits[it] to digits[it + 1] }
    }
}
