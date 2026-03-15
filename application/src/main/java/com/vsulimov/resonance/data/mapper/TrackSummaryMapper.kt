package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.resonance.domain.model.TrackSummary

/**
 * Maps libsubsonic [Child] to the domain [TrackSummary] model.
 *
 * Handles nullable fields from the API by falling back to sensible defaults
 * so the domain layer always works with non-null values where expected.
 */
object TrackSummaryMapper {

    private const val UNKNOWN_ARTIST = "Unknown Artist"

    /**
     * Converts a single [Child] into a [TrackSummary].
     *
     * @param child The raw song data from the Subsonic API.
     * @return A domain model with [artistName][TrackSummary.artistName] guaranteed non-null.
     */
    fun map(child: Child): TrackSummary = TrackSummary(
        id = child.id,
        title = child.title,
        artistName = child.artist ?: UNKNOWN_ARTIST,
        trackNumber = child.track,
        durationSeconds = child.duration
    )

    /**
     * Converts a list of [Child] instances into [TrackSummary] instances.
     *
     * @param children The raw song list from the Subsonic API.
     * @return A list of domain models.
     */
    fun mapList(children: List<Child>): List<TrackSummary> = children.map(::map)
}
