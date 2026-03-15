package com.vsulimov.resonance.domain.repository

import android.graphics.Bitmap

/**
 * Abstraction for retrieving album cover art images.
 *
 * Implementations should cache images in memory to avoid redundant
 * network requests when the same cover art is displayed multiple times
 * (e.g. across tab switches or scroll restoration).
 */
interface CoverArtRepository {

    /**
     * Retrieves the cover art image for the given identifier.
     *
     * @param id Cover art identifier from [AlbumSummary.coverArtId][com.vsulimov.resonance.domain.model.AlbumSummary.coverArtId].
     * @param sizePx Requested image size in pixels (width and height).
     * @return [Result.success] with a decoded [Bitmap], or
     *   [Result.failure] if the image could not be fetched or decoded.
     */
    suspend fun getCoverArt(id: String, sizePx: Int): Result<Bitmap>

    /**
     * Returns the current in-memory cache size in bytes.
     *
     * This reflects only the memory LRU cache, not any future disk cache.
     */
    fun getCacheSizeBytes(): Long

    /**
     * Evicts all entries from the in-memory cover art cache.
     */
    fun clearCache()
}
