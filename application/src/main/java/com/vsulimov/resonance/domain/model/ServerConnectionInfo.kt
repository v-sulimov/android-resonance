package com.vsulimov.resonance.domain.model

/**
 * Domain model representing metadata returned by a successful server ping.
 *
 * @property serverVersion Version string of the Navidrome server (e.g. "0.53.3").
 * @property apiVersion Subsonic REST API version supported by the server (e.g. "1.16.1").
 * @property serverType Server implementation identifier (e.g. "navidrome").
 * @property isOpenSubsonic Whether the server advertises OpenSubsonic support.
 */
data class ServerConnectionInfo(
    val serverVersion: String,
    val apiVersion: String,
    val serverType: String,
    val isOpenSubsonic: Boolean
)
