package com.vsulimov.resonance.data.credentials

/**
 * Represents the core server connection and authentication credentials required by libsubsonic.
 *
 * @property serverUrl The base URL of the Subsonic server.
 * @property username The username used for authentication.
 * @property passwordPlain The plain text password used for authentication.
 */
data class SubsonicCredentials(val serverUrl: String, val username: String, val passwordPlain: String)
