package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.domain.model.ServerCredentials
import com.vsulimov.resonance.domain.repository.CredentialsRepository
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import com.vsulimov.resonance.domain.repository.ServerRepository

/**
 * Verifies server connectivity and persists credentials on success.
 *
 * Orchestrates a ping request via [ServerRepository] and, when the server
 * responds successfully, saves the credentials through [CredentialsRepository]
 * and the server metadata through [PreferencesRepository] so they are
 * available on subsequent app launches.
 *
 * @param serverRepository Repository handling the network ping.
 * @param credentialsRepository Repository for secure credential persistence.
 * @param preferencesRepository Repository for user preferences and server metadata.
 */
class ConnectToServerUseCase(
    private val serverRepository: ServerRepository,
    private val credentialsRepository: CredentialsRepository,
    private val preferencesRepository: PreferencesRepository
) {

    /**
     * Pings the server with the given connection details.
     *
     * On success the credentials and server metadata are persisted to
     * storage before returning the result. On failure nothing is saved.
     *
     * @param url Base URL of the Subsonic-compatible server.
     * @param username Account username.
     * @param password Account password in plain text.
     * @return [Result.success] with [ServerConnectionInfo] when the server
     *   is reachable and credentials are valid, or [Result.failure] otherwise.
     */
    suspend operator fun invoke(
        url: String,
        username: String,
        password: String
    ): Result<ServerConnectionInfo> {
        val credentials = ServerCredentials(
            serverUrl = url,
            username = username,
            password = password
        )
        val connectionResult = serverRepository.connect(credentials)
        if (connectionResult.isSuccess) {
            try {
                credentialsRepository.save(credentials)
                val info = connectionResult.getOrThrow()
                preferencesRepository.setServerVersion(info.serverVersion)
                preferencesRepository.setServerType(info.serverType)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return connectionResult
    }
}
