package com.gymvip.mobile.presentation.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Local screen gate. Authentication is provided by the host platform, never by the server token. */
object LocalBiometricGate {
    private val mutableLocked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = mutableLocked
    var enabled: Boolean = false
    var isAvailable: () -> Boolean = { false }
    var authenticate: ((Boolean) -> Unit) -> Unit = { it(false) }
    var requireLogin: () -> Unit = {}

    fun lock() { if (enabled) mutableLocked.value = true }
    fun unlock() { mutableLocked.value = false }
}
