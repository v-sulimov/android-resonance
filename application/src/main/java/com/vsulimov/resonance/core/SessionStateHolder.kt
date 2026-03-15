package com.vsulimov.resonance.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide holder for session lifecycle events.
 *
 * When the server rejects stored credentials (e.g. wrong password after
 * a remote password change), any component can call [notifySessionExpired]
 * to trigger a global logout. Observers of [sessionExpired] (typically the
 * top-level navigation composable) react by clearing state and redirecting
 * the user to the onboarding flow.
 */
class SessionStateHolder {

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Flow that emits a single [Unit] whenever the current session
     * is invalidated due to an authentication failure.
     *
     * Collectors should navigate the user to the onboarding flow
     * and discard any in-memory state tied to the previous session.
     */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /**
     * Signals that the stored credentials are no longer valid.
     *
     * This is a fire-and-forget call — it uses [MutableSharedFlow.tryEmit]
     * so it never suspends or blocks.
     */
    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
