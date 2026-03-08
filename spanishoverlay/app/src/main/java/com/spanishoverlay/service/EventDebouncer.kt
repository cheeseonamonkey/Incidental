package com.spanishoverlay.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class EventDebouncer(
    private val delayMsFlow: StateFlow<Int>,
    private val scope: CoroutineScope
) {
    private var pending: Job? = null

    fun debounce(block: suspend () -> Unit) {
        pending?.cancel()
        pending = scope.launch {
            delay(delayMsFlow.value.toLong().coerceIn(50, 2000))
            block()
        }
    }

    fun cancel() { pending?.cancel() }
}
