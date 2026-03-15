package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Child
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackSummaryMapperTest {

    @Test
    fun `map converts all fields correctly`() {
        val child = createChild(
            id = "song-1",
            title = "Track Title",
            artist = "Track Artist",
            track = 3,
            duration = 210
        )

        val result = TrackSummaryMapper.map(child)

        assertEquals("song-1", result.id)
        assertEquals("Track Title", result.title)
        assertEquals("Track Artist", result.artistName)
        assertEquals(3, result.trackNumber)
        assertEquals(210, result.durationSeconds)
    }

    @Test
    fun `map uses Unknown Artist when artist is null`() {
        val child = createChild(artist = null)

        val result = TrackSummaryMapper.map(child)

        assertEquals("Unknown Artist", result.artistName)
    }

    @Test
    fun `map handles null track number`() {
        val child = createChild(track = null)

        val result = TrackSummaryMapper.map(child)

        assertEquals(null, result.trackNumber)
    }

    @Test
    fun `map handles null duration`() {
        val child = createChild(duration = null)

        val result = TrackSummaryMapper.map(child)

        assertEquals(null, result.durationSeconds)
    }

    @Test
    fun `mapList converts all items`() {
        val children = listOf(
            createChild(id = "s-1", title = "First"),
            createChild(id = "s-2", title = "Second"),
            createChild(id = "s-3", title = "Third")
        )

        val result = TrackSummaryMapper.mapList(children)

        assertEquals(3, result.size)
        assertEquals("First", result[0].title)
        assertEquals("Second", result[1].title)
        assertEquals("Third", result[2].title)
    }

    private fun createChild(
        id: String = "song-1",
        title: String = "Song Title",
        artist: String? = "Artist",
        track: Int? = 1,
        duration: Int? = 180
    ): Child = Child(
        id = id,
        isDir = false,
        title = title,
        artist = artist,
        track = track,
        duration = duration
    )
}
