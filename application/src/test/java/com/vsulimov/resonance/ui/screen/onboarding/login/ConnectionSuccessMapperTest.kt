package com.vsulimov.resonance.ui.screen.onboarding.login

import com.vsulimov.resonance.domain.model.ServerConnectionInfo
import com.vsulimov.resonance.ui.mapper.ConnectionSuccessMapper
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [ConnectionSuccessMapper].
 *
 * Verifies that domain model fields are correctly mapped to the
 * presentation-layer [ConnectionSuccessViewObject].
 */
class ConnectionSuccessMapperTest {

    @Test
    fun `map extracts serverVersion from domain model`() {
        val info = ServerConnectionInfo(
            serverVersion = "0.53.3",
            apiVersion = "1.16.1",
            serverType = "navidrome",
            isOpenSubsonic = true
        )

        val viewObject = ConnectionSuccessMapper.map(info)

        assertEquals("0.53.3", viewObject.serverVersion)
    }

    @Test
    fun `map preserves unknown serverVersion`() {
        val info = ServerConnectionInfo(
            serverVersion = "unknown",
            apiVersion = "1.16.1",
            serverType = "subsonic",
            isOpenSubsonic = false
        )

        val viewObject = ConnectionSuccessMapper.map(info)

        assertEquals("unknown", viewObject.serverVersion)
    }
}
