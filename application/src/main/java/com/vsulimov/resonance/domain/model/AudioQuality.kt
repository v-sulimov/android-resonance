package com.vsulimov.resonance.domain.model

/**
 * Audio streaming quality preference.
 *
 * Determines whether the server should transcode audio before streaming.
 * When playback is implemented, this value will be sent as the `maxBitRate`
 * parameter to the Subsonic `stream` endpoint.
 *
 * @property maxBitRate Maximum bit rate in kbps, or `0` for no transcoding.
 */
enum class AudioQuality(val maxBitRate: Int) {

    /** No transcoding — stream the original file. */
    ORIGINAL(maxBitRate = 0),

    /** Transcode to 320 kbps MP3. */
    HIGH(maxBitRate = 320),

    /** Transcode to 192 kbps MP3. */
    MEDIUM(maxBitRate = 192),

    /** Transcode to 128 kbps MP3. */
    LOW(maxBitRate = 128)
}
