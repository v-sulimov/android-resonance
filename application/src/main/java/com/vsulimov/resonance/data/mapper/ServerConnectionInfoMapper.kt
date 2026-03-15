package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.system.PingResponse
import com.vsulimov.resonance.domain.model.ServerConnectionInfo

/**
 * Maps libsubsonic [PingResponse] to the domain [ServerConnectionInfo] model.
 *
 * Handles nullable fields from the API by falling back to sensible defaults
 * so the domain layer always works with non-null values.
 */
object ServerConnectionInfoMapper {

    private const val UNKNOWN_VERSION = "unknown"
    private const val UNKNOWN_TYPE = "unknown"

    /**
     * Converts a [PingResponse] into a [ServerConnectionInfo].
     *
     * @param response The raw ping response from the Subsonic API.
     * @return A domain model with all fields guaranteed non-null.
     */
    fun map(response: PingResponse): ServerConnectionInfo = ServerConnectionInfo(
        serverVersion = response.serverVersion ?: UNKNOWN_VERSION,
        apiVersion = response.apiVersion,
        serverType = response.serverType ?: UNKNOWN_TYPE,
        isOpenSubsonic = response.isOpenSubsonic
    )
}
