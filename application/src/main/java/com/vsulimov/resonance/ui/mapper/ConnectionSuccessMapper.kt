package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.ui.screen.onboarding.login.ConnectionSuccessViewObject

/**
 * Maps domain [ServerConnectionInfo] to [ConnectionSuccessViewObject]
 * suitable for direct consumption by Compose UI.
 */
object ConnectionSuccessMapper {

    /**
     * Converts a [ServerConnectionInfo] into a [ConnectionSuccessViewObject].
     *
     * @param info The domain model returned by the connect use case.
     * @return A view object with fields formatted for display.
     */
    fun map(info: ServerConnectionInfo): ConnectionSuccessViewObject =
        ConnectionSuccessViewObject(serverVersion = info.serverVersion)
}
