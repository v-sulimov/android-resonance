package com.vsulimov.resonance.data.db

/**
 * Defines the current download state of a track or media resource.
 */
enum class DownloadStatus {
    /**
     * Item is not queued or downloaded.
     */
    NOT_DOWNLOADED,

    /**
     * Item is added to download queue awaiting processing.
     */
    QUEUED,

    /**
     * Item is currently downloading byte stream from network.
     */
    DOWNLOADING,

    /**
     * Item successfully downloaded and persisted locally.
     */
    DOWNLOADED,

    /**
     * Download encountered an unrecoverable error.
     */
    FAILED
}
