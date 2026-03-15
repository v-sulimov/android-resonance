package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Child
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlbumDetailMapperTest {

    @Test
    fun `map converts all fields correctly`() {
        val album = createAlbum(
            id = "album-1",
            name = "Test Album",
            artist = "Test Artist",
            coverArt = "cover-1",
            year = 2023,
            genre = "Rock",
            songCount = 10,
            duration = 2400
        )

        val result = AlbumDetailMapper.map(album)

        assertEquals("album-1", result.id)
        assertEquals("Test Album", result.name)
        assertEquals("Test Artist", result.artistName)
        assertEquals("cover-1", result.coverArtId)
        assertEquals(2023, result.year)
        assertEquals("Rock", result.genre)
        assertEquals(10, result.songCount)
        assertEquals(2400, result.durationSeconds)
    }

    @Test
    fun `map uses Unknown Artist when artist is null`() {
        val album = createAlbum(artist = null)

        val result = AlbumDetailMapper.map(album)

        assertEquals("Unknown Artist", result.artistName)
    }

    @Test
    fun `map handles null coverArt`() {
        val album = createAlbum(coverArt = null)

        val result = AlbumDetailMapper.map(album)

        assertNull(result.coverArtId)
    }

    @Test
    fun `map handles null year`() {
        val album = createAlbum(year = null)

        val result = AlbumDetailMapper.map(album)

        assertNull(result.year)
    }

    @Test
    fun `map uses songs size when songCount is null`() {
        val songs = listOf(
            createChild(id = "s-1"),
            createChild(id = "s-2"),
            createChild(id = "s-3")
        )
        val album = createAlbum(songCount = null, songs = songs)

        val result = AlbumDetailMapper.map(album)

        assertEquals(3, result.songCount)
    }

    @Test
    fun `map maps songs to tracks`() {
        val songs = listOf(
            createChild(id = "s-1", title = "First Song", track = 1),
            createChild(id = "s-2", title = "Second Song", track = 2)
        )
        val album = createAlbum(songs = songs)

        val result = AlbumDetailMapper.map(album)

        assertEquals(2, result.tracks.size)
        assertEquals("First Song", result.tracks[0].title)
        assertEquals("Second Song", result.tracks[1].title)
        assertEquals(1, result.tracks[0].trackNumber)
        assertEquals(2, result.tracks[1].trackNumber)
    }

    private fun createAlbum(
        id: String = "album-1",
        name: String = "Album",
        artist: String? = "Artist",
        coverArt: String? = "cover-1",
        year: Int? = 2023,
        genre: String? = "Rock",
        songCount: Int? = 0,
        duration: Int? = 0,
        songs: List<Child> = emptyList()
    ): Album = Album(
        id = id,
        name = name,
        artist = artist,
        coverArt = coverArt,
        year = year,
        genre = genre,
        songCount = songCount,
        duration = duration,
        songs = songs
    )

    private fun createChild(
        id: String = "song-1",
        title: String = "Song",
        track: Int? = 1
    ): Child = Child(
        id = id,
        isDir = false,
        title = title,
        track = track
    )
}
