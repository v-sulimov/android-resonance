package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.TrackSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackViewObjectMapperTest {

    @Test
    fun `map converts all fields correctly`() {
        val track = TrackSummary(
            id = "song-1",
            title = "Track Title",
            artistName = "Artist",
            trackNumber = 5,
            durationSeconds = 225
        )

        val result = TrackViewObjectMapper.map(track)

        assertEquals("song-1", result.id)
        assertEquals("Track Title", result.title)
        assertEquals("5", result.trackNumber)
        assertEquals("3:45", result.formattedDuration)
    }

    @Test
    fun `map formats duration with leading zero on seconds`() {
        val track = createTrack(durationSeconds = 303)

        val result = TrackViewObjectMapper.map(track)

        assertEquals("5:03", result.formattedDuration)
    }

    @Test
    fun `map formats short duration`() {
        val track = createTrack(durationSeconds = 30)

        val result = TrackViewObjectMapper.map(track)

        assertEquals("0:30", result.formattedDuration)
    }

    @Test
    fun `map formats long duration`() {
        val track = createTrack(durationSeconds = 725)

        val result = TrackViewObjectMapper.map(track)

        assertEquals("12:05", result.formattedDuration)
    }

    @Test
    fun `map returns empty string for null track number`() {
        val track = createTrack(trackNumber = null)

        val result = TrackViewObjectMapper.map(track)

        assertEquals("", result.trackNumber)
    }

    @Test
    fun `map returns empty string for null duration`() {
        val track = createTrack(durationSeconds = null)

        val result = TrackViewObjectMapper.map(track)

        assertEquals("", result.formattedDuration)
    }

    @Test
    fun `mapList converts all items`() {
        val tracks = listOf(
            createTrack(id = "s-1"),
            createTrack(id = "s-2")
        )

        val result = TrackViewObjectMapper.mapList(tracks)

        assertEquals(2, result.size)
        assertEquals("s-1", result[0].id)
        assertEquals("s-2", result[1].id)
    }

    private fun createTrack(
        id: String = "song-1",
        title: String = "Title",
        artistName: String = "Artist",
        trackNumber: Int? = 1,
        durationSeconds: Int? = 180
    ): TrackSummary = TrackSummary(
        id = id,
        title = title,
        artistName = artistName,
        trackNumber = trackNumber,
        durationSeconds = durationSeconds
    )
}
