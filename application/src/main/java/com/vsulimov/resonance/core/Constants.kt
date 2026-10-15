package com.vsulimov.resonance.core

/**
 * Global application constants to prevent magic strings and numbers.
 */
object Constants {
    /** The client name sent to the Subsonic API to identify this app. */
    const val APP_CLIENT_NAME = "Resonance"

    /** HTTPS protocol prefix. */
    const val PROTOCOL_HTTPS = "https://"

    /** HTTP protocol prefix. */
    const val PROTOCOL_HTTP = "http://"

    /** Divisor used to calculate the max memory limit for the LruCache (e.g., 1/8th). */
    const val MEMORY_CACHE_DIVISOR = 8

    /** Kilobyte multiplier (1024 bytes). */
    const val KILOBYTE = 1024

    /** JPEG compression quality (0-100) for disk cache. */
    const val DISK_CACHE_QUALITY = 85

    /** MD5 Hash Algorithm name */
    const val ALGORITHM_MD5 = "MD5"

    /** Recommended thumbnail size when requesting cover art. */
    const val COVER_ART_THUMBNAIL_SIZE = 300

    /** The number of items to fetch for home screen carousels. */
    const val HOME_CAROUSEL_ITEM_COUNT = 15
}
