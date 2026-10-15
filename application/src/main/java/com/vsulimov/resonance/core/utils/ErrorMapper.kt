package com.vsulimov.resonance.core.utils

import com.vsulimov.libsubsonic.data.result.error.SubsonicError
import com.vsulimov.libsubsonic.data.result.error.SubsonicErrorCode
import com.vsulimov.resonance.R
import com.vsulimov.resonance.core.ui.UiText
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Utility object for mapping low-level exceptions and API error codes to localized [UiText] representations.
 */
object ErrorMapper {

    /**
     * Maps a [SubsonicError] to a localized [UiText] representation.
     *
     * @param error The [SubsonicError] returned by the API client.
     * @return [UiText] representing the error message.
     */
    fun formatError(error: SubsonicError): UiText = when (error.code) {
        SubsonicErrorCode.WRONG_CREDENTIALS_LEGACY,
        SubsonicErrorCode.WRONG_CREDENTIALS,
        SubsonicErrorCode.NOT_AUTHORIZED
        -> UiText.StringResource(R.string.status_error_auth_failed)

        SubsonicErrorCode.GENERIC_ERROR -> {
            if (isNetworkMessage(error.message)) {
                UiText.StringResource(R.string.error_network_connection)
            } else if (error.message.isNotBlank()) {
                UiText.DynamicString(error.message)
            } else {
                UiText.StringResource(R.string.error_loading_failed)
            }
        }

        else -> {
            if (error.message.isNotBlank()) {
                UiText.DynamicString(error.message)
            } else {
                UiText.StringResource(R.string.error_loading_failed)
            }
        }
    }

    /**
     * Maps a [Throwable] exception to a localized [UiText] representation.
     *
     * @param throwable The exception thrown during a network or API operation.
     * @return [UiText] representing the error message.
     */
    fun formatError(throwable: Throwable): UiText = when {
        isNetworkException(throwable) -> UiText.StringResource(R.string.error_network_connection)

        throwable is SubsonicError -> formatError(throwable)

        else -> {
            val message = throwable.message ?: ""
            if (isNetworkMessage(message)) {
                UiText.StringResource(R.string.error_network_connection)
            } else {
                UiText.StringResource(R.string.error_loading_failed)
            }
        }
    }

    /**
     * Determines whether the given throwable represents a network connectivity failure.
     *
     * @param throwable The exception or error to check.
     * @return True if the error is caused by missing network or host connection issues.
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        if (isNetworkException(throwable)) return true
        if (throwable is SubsonicError && (throwable.code == SubsonicErrorCode.GENERIC_ERROR) &&
            isNetworkMessage(throwable.message)
        ) {
            return true
        }
        return isNetworkMessage(throwable.message ?: "")
    }

    /**
     * Checks whether a throwable matches well-known network connection exception types.
     *
     * @param throwable Throwable to inspect.
     * @return True if throwable represents a network layer exception.
     */
    private fun isNetworkException(throwable: Throwable): Boolean = throwable is UnknownHostException ||
        throwable is ConnectException ||
        throwable is SocketTimeoutException ||
        throwable is SocketException ||
        throwable is SSLException ||
        (throwable is IOException && isNetworkMessage(throwable.message ?: ""))

    /**
     * Inspects exception message for common network failure patterns.
     *
     * @param message Message string from exception or error.
     * @return True if message indicates a connection or host resolution failure.
     */
    private fun isNetworkMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") ||
            lower.contains("connection refused") ||
            lower.contains("network is unreachable") ||
            lower.contains("software caused connection abort") ||
            lower.contains("connection reset") ||
            lower.contains("timeout") ||
            lower.contains("standalone") ||
            lower.contains("hostname")
    }
}
