package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.domain.model.SongSummary

/**
 * Maps libsubsonic [Child] to the domain [SongSummary] model.
 *
 * Handles nullable fields from the API by falling back to sensible defaults
 * so the domain layer always works with non-null values where expected.
 */
object SongSummaryMapper {

    private const val UNKNOWN_ARTIST = "Unknown Artist"
    private const val UNKNOWN_ALBUM = "Unknown Album"

    /**
     * Converts a single [Child] into a [SongSummary].
     *
     * @param child The raw song data from the Subsonic API.
     * @return A domain model with [artistName] and [albumName] guaranteed non-null.
     */
    fun map(child: Child): SongSummary = SongSummary(
        id = child.id,
        title = child.title,
        artistName = child.artist ?: UNKNOWN_ARTIST,
        albumName = child.album ?: UNKNOWN_ALBUM,
        albumId = child.albumId,
        coverArtId = child.coverArt,
        durationSeconds = child.duration
    )

    /**
     * Converts a list of [Child] instances into [SongSummary] instances.
     *
     * @param children The raw song list from the Subsonic API.
     * @return A list of domain models.
     */
    fun mapList(children: List<Child>): List<SongSummary> = children.map(::map)
}
