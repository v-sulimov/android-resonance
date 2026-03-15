package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlbumSummaryMapperTest {

    @Test
    fun `map copies id and name from album`() {
        val album = createAlbum(id = "al-1", name = "OK Computer")
        val result = AlbumSummaryMapper.map(album)
        assertEquals("al-1", result.id)
        assertEquals("OK Computer", result.name)
    }

    @Test
    fun `map uses artist name when present`() {
        val album = createAlbum(artist = "Radiohead")
        val result = AlbumSummaryMapper.map(album)
        assertEquals("Radiohead", result.artistName)
    }

    @Test
    fun `map falls back to Unknown Artist when artist is null`() {
        val album = createAlbum(artist = null)
        val result = AlbumSummaryMapper.map(album)
        assertEquals("Unknown Artist", result.artistName)
    }

    @Test
    fun `map preserves coverArt when present`() {
        val album = createAlbum(coverArt = "cover-42")
        val result = AlbumSummaryMapper.map(album)
        assertEquals("cover-42", result.coverArtId)
    }

    @Test
    fun `map preserves null coverArt`() {
        val album = createAlbum(coverArt = null)
        val result = AlbumSummaryMapper.map(album)
        assertNull(result.coverArtId)
    }

    @Test
    fun `mapList converts all items`() {
        val albums = listOf(
            createAlbum(id = "1", name = "A"),
            createAlbum(id = "2", name = "B"),
            createAlbum(id = "3", name = "C")
        )
        val result = AlbumSummaryMapper.mapList(albums)
        assertEquals(3, result.size)
        assertEquals("A", result[0].name)
        assertEquals("B", result[1].name)
        assertEquals("C", result[2].name)
    }

    private fun createAlbum(
        id: String = "id",
        name: String = "name",
        artist: String? = "artist",
        coverArt: String? = "cover"
    ): Album = Album(
        id = id,
        name = name,
        artist = artist,
        coverArt = coverArt
    )
}
