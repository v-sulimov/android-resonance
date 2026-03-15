package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.mapper.ServerConnectionInfoMapper
import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.domain.model.ServerCredentials
import com.vsulimov.resonance.domain.repository.ServerRepository

/**
 * [ServerRepository] implementation backed by [SubsonicClient].
 *
 * A new [SubsonicClient] instance is created for each connection attempt
 * because the base URL is only known at call time and is set via the
 * constructor.
 */
class ServerRepositoryImpl : ServerRepository {

    /**
     * Creates a [SubsonicClient] configured with the given [credentials],
     * performs a ping request, and maps the result to the domain model
     * using [ServerConnectionInfoMapper].
     *
     * @param credentials Server URL and authentication details.
     * @return [Result.success] with [ServerConnectionInfo] on a successful ping,
     *   or [Result.failure] wrapping the [SubsonicError][com.vsulimov.libsubsonic.data.result.error.SubsonicError]
     *   on failure.
     */
    override suspend fun connect(credentials: ServerCredentials): Result<ServerConnectionInfo> {
        val client = SubsonicClient(baseUrl = credentials.serverUrl)
        client.setCredentials(
            username = credentials.username,
            password = credentials.password
        )
        return when (val result = client.ping()) {
            is SubsonicResult.Success -> Result.success(ServerConnectionInfoMapper.map(result.data))
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }
}
