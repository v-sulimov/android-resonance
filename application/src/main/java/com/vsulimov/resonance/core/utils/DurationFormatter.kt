package com.vsulimov.resonance.core.utils

import java.util.Locale

/**
 * Utility functions for formatting time durations into user-friendly strings.
 */
object DurationFormatter {

    /** Number of seconds in a standard minute. */
    private const val SECONDS_PER_MINUTE = 60

    /** Number of minutes in a standard hour. */
    private const val MINUTES_PER_HOUR = 60

    /** Total number of seconds in a standard hour. */
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR

    /** Default placeholder string returned when the duration is invalid or negative. */
    private const val DEFAULT_DURATION_PLACEHOLDER = "--:--"

    /**
     * Formats duration given in seconds into standard time format string (e.g. "3:45" or "1:12:30").
     *
     * @param seconds Total seconds duration.
     * @return Formatted duration string or "--:--" if null or negative.
     */
    fun formatSeconds(seconds: Int?): String {
        if (seconds == null || seconds < 0) {
            return DEFAULT_DURATION_PLACEHOLDER
        }

        val hours = seconds / SECONDS_PER_HOUR
        val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
        }
    }
}
