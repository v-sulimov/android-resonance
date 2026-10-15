package com.vsulimov.resonance.data.credentials

/**
 * Represents the core server connection and authentication credentials required by libsubsonic.
 */
data class SubsonicCredentials(val serverUrl: String, val username: String, val passwordPlain: String)
