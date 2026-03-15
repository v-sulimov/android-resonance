package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.AlbumSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlbumCardMapperTest {

    @Test
    fun `map copies all fields from domain model`() {
        val summary = AlbumSummary(
            id = "al-1",
            name = "In Rainbows",
            artistName = "Radiohead",
            coverArtId = "cover-1"
        )
        val result = AlbumCardMapper.map(summary)
        assertEquals("al-1", result.id)
        assertEquals("In Rainbows", result.name)
        assertEquals("Radiohead", result.artistName)
        assertEquals("cover-1", result.coverArtId)
    }

    @Test
    fun `map preserves null coverArtId`() {
        val summary = AlbumSummary(
            id = "al-2",
            name = "Kid A",
            artistName = "Radiohead",
            coverArtId = null
        )
        val result = AlbumCardMapper.map(summary)
        assertNull(result.coverArtId)
    }

    @Test
    fun `mapList converts all items`() {
        val summaries = listOf(
            AlbumSummary("1", "A", "X", "c1"),
            AlbumSummary("2", "B", "Y", null)
        )
        val result = AlbumCardMapper.mapList(summaries)
        assertEquals(2, result.size)
        assertEquals("A", result[0].name)
        assertEquals("B", result[1].name)
    }
}
