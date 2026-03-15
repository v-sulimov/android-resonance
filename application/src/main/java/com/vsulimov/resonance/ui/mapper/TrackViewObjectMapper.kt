package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.TrackSummary
import com.vsulimov.resonance.ui.screen.albumdetail.TrackViewObject

/**
 * Maps domain [TrackSummary] to presentation [TrackViewObject].
 *
 * Formats numeric values into display-ready strings so that
 * composable functions do not contain formatting logic.
 */
object TrackViewObjectMapper {

    /**
     * Converts a single [TrackSummary] into a [TrackViewObject].
     *
     * @param track The domain model to convert.
     * @return A view object with pre-formatted display strings.
     */
    fun map(track: TrackSummary): TrackViewObject = TrackViewObject(
        id = track.id,
        trackNumber = track.trackNumber?.toString() ?: "",
        title = track.title,
        formattedDuration = track.durationSeconds?.let { formatDuration(it) } ?: ""
    )

    /**
     * Converts a list of [TrackSummary] instances into [TrackViewObject] instances.
     *
     * @param tracks The domain models to convert.
     * @return A list of view objects with pre-formatted display strings.
     */
    fun mapList(tracks: List<TrackSummary>): List<TrackViewObject> = tracks.map(::map)

    /**
     * Formats a duration in seconds to "m:ss" format.
     *
     * @param seconds Total duration in seconds.
     * @return Formatted string (e.g. "3:45", "0:30", "12:05").
     */
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val remainingSeconds = seconds % SECONDS_PER_MINUTE
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    private const val SECONDS_PER_MINUTE = 60
}
