package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.system.PingResponse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [ServerConnectionInfoMapper].
 *
 * Verifies that [PingResponse] fields are correctly mapped to the
 * domain [ServerConnectionInfo][com.vsulimov.resonance.domain.model.ServerConnectionInfo]
 * model, including nullable field fallback behavior.
 */
class ServerConnectionInfoMapperTest {

    @Test
    fun `map converts all non-null fields correctly`() {
        val response = PingResponse(
            status = "ok",
            apiVersion = "1.16.1",
            serverType = "navidrome",
            serverVersion = "0.53.3",
            isOpenSubsonic = true
        )

        val result = ServerConnectionInfoMapper.map(response)

        assertEquals("0.53.3", result.serverVersion)
        assertEquals("1.16.1", result.apiVersion)
        assertEquals("navidrome", result.serverType)
        assertEquals(true, result.isOpenSubsonic)
    }

    @Test
    fun `map falls back to unknown when serverVersion is null`() {
        val response = PingResponse(
            status = "ok",
            apiVersion = "1.16.1",
            serverType = "navidrome",
            serverVersion = null,
            isOpenSubsonic = false
        )

        val result = ServerConnectionInfoMapper.map(response)

        assertEquals("unknown", result.serverVersion)
    }

    @Test
    fun `map falls back to unknown when serverType is null`() {
        val response = PingResponse(
            status = "ok",
            apiVersion = "1.16.1",
            serverType = null,
            serverVersion = "0.53.3",
            isOpenSubsonic = false
        )

        val result = ServerConnectionInfoMapper.map(response)

        assertEquals("unknown", result.serverType)
    }

    @Test
    fun `map falls back to unknown when both nullable fields are null`() {
        val response = PingResponse(
            status = "ok",
            apiVersion = "1.16.1",
            serverType = null,
            serverVersion = null,
            isOpenSubsonic = false
        )

        val result = ServerConnectionInfoMapper.map(response)

        assertEquals("unknown", result.serverVersion)
        assertEquals("unknown", result.serverType)
        assertEquals("1.16.1", result.apiVersion)
        assertEquals(false, result.isOpenSubsonic)
    }
}
