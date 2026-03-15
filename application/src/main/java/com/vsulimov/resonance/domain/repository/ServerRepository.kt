package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.domain.model.ServerCredentials

/**
 * Abstraction for verifying connectivity with a Subsonic-compatible server.
 *
 * Implementations handle client creation, credential configuration, and the
 * network ping call, returning a domain-level result.
 */
interface ServerRepository {

    /**
     * Attempts to connect to a server using the provided [credentials].
     *
     * Creates a configured Subsonic client, sets credentials, and performs
     * a ping request to verify that the server is reachable and the
     * credentials are valid.
     *
     * @param credentials Server URL and authentication details.
     * @return [Result.success] with [ServerConnectionInfo] on a successful ping,
     *   or [Result.failure] with the underlying error on failure.
     */
    suspend fun connect(credentials: ServerCredentials): Result<ServerConnectionInfo>
}
