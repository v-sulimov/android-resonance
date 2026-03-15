package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.AlbumDetail
import com.vsulimov.resonance.domain.model.TrackSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlbumDetailViewObjectMapperTest {

    @Test
    fun `map converts basic fields correctly`() {
        val album = createAlbum(
            id = "album-1",
            name = "Test Album",
            artistName = "Test Artist",
            coverArtId = "cover-1"
        )

        val result = AlbumDetailViewObjectMapper.map(album)

        assertEquals("album-1", result.id)
        assertEquals("Test Album", result.name)
        assertEquals("Test Artist", result.artistName)
        assertEquals("cover-1", result.coverArtId)
    }

    @Test
    fun `map passes through year`() {
        val album = createAlbum(year = 2023)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertEquals(2023, result.year)
    }

    @Test
    fun `map passes through null year`() {
        val album = createAlbum(year = null)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertNull(result.year)
    }

    @Test
    fun `map passes through song count when positive`() {
        val album = createAlbum(songCount = 12)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertEquals(12, result.songCount)
    }

    @Test
    fun `map omits song count when zero`() {
        val album = createAlbum(songCount = 0)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertNull(result.songCount)
    }

    @Test
    fun `map passes through duration seconds`() {
        val album = createAlbum(durationSeconds = 2700)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertEquals(2700, result.durationSeconds)
    }

    @Test
    fun `map passes through null duration`() {
        val album = createAlbum(durationSeconds = null)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertNull(result.durationSeconds)
    }

    @Test
    fun `map converts tracks`() {
        val tracks = listOf(
            TrackSummary("s-1", "First", "Artist", 1, 180),
            TrackSummary("s-2", "Second", "Artist", 2, 240)
        )
        val album = createAlbum(tracks = tracks)

        val result = AlbumDetailViewObjectMapper.map(album)

        assertEquals(2, result.tracks.size)
        assertEquals("First", result.tracks[0].title)
        assertEquals("Second", result.tracks[1].title)
    }

    private fun createAlbum(
        id: String = "album-1",
        name: String = "Album",
        artistName: String = "Artist",
        coverArtId: String? = "cover-1",
        year: Int? = 2023,
        songCount: Int = 10,
        durationSeconds: Int? = 2400,
        tracks: List<TrackSummary> = emptyList()
    ): AlbumDetail = AlbumDetail(
        id = id,
        name = name,
        artistName = artistName,
        coverArtId = coverArtId,
        year = year,
        genre = null,
        songCount = songCount,
        durationSeconds = durationSeconds,
        tracks = tracks
    )
}
