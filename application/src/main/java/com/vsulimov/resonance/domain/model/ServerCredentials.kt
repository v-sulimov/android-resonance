package com.vsulimov.resonance.domain.model

/**
 * Credentials required to authenticate with a Subsonic-compatible server.
 *
 * @property serverUrl Base URL of the server (e.g. "https://music.example.com").
 * @property username Account username.
 * @property password Account password in plain text. Hex-encoding for the wire
 *   format is handled by the Subsonic client library.
 */
data class ServerCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)
