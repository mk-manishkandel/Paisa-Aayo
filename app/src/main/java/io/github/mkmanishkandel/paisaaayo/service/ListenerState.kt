package io.github.mkmanishkandel.paisaaayo.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-wide view of whether Android currently has our listener bound. */
object ListenerState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    internal fun set(value: Boolean) {
        _connected.value = value
    }
}
