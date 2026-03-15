package com.vsulimov.resonance.domain.repository

/**
 * Discards the cached API client so that the next API call
 * creates a fresh instance with current credentials.
 *
 * Used during logout to ensure the old session is not reused.
 */
fun interface ClientInvalidator {

    /** Invalidates the cached client instance. */
    fun invalidate()
}
