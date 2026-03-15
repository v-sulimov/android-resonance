package com.vsulimov.resonance.domain.model

/**
 * User preference for the maximum disk cache size.
 *
 * Applied when disk caching is available (e.g. for audio streams
 * and cover art). The actual enforcement is handled by the data layer.
 *
 * @property bytes Maximum cache size in bytes, or [Long.MAX_VALUE] for unlimited.
 */
enum class CacheLimit(val bytes: Long) {

    /** 256 MB — minimal storage usage. */
    MB_256(bytes = 256L * 1024 * 1024),

    /** 512 MB. */
    MB_512(bytes = 512L * 1024 * 1024),

    /** 1 GB — recommended default. */
    GB_1(bytes = 1024L * 1024 * 1024),

    /** 2 GB. */
    GB_2(bytes = 2L * 1024 * 1024 * 1024),

    /** 5 GB — suited for large libraries with offline playback. */
    GB_5(bytes = 5L * 1024 * 1024 * 1024),

    /** No limit — uses all available space. */
    UNLIMITED(bytes = Long.MAX_VALUE)
}
