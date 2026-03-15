package com.vsulimov.resonance.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.repository.CoverArtRepository

/**
 * [CoverArtRepository] implementation backed by the Subsonic API with
 * an in-memory LRU cache.
 *
 * Images are cached by a composite key of `"id:sizePx"` so that
 * different requested sizes are stored independently. The cache evicts
 * least-recently-used entries when the total byte size exceeds
 * [MAX_CACHE_SIZE_BYTES].
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class CoverArtRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : CoverArtRepository {

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    /**
     * Returns the cover art image, serving from cache when available.
     *
     * On a cache miss, fetches the image from the server via
     * [SubsonicClient.getCoverArt][com.vsulimov.libsubsonic.SubsonicClient.getCoverArt],
     * decodes the [InputStream][java.io.InputStream] with [BitmapFactory], and
     * stores the result in the LRU cache before returning.
     *
     * @param id Cover art identifier.
     * @param sizePx Requested image dimensions in pixels.
     * @return [Result.success] with the decoded [Bitmap], or [Result.failure]
     *   if the client is unavailable, the request fails, or decoding fails.
     */
    override suspend fun getCoverArt(id: String, sizePx: Int): Result<Bitmap> {
        val cacheKey = "$id:$sizePx"
        cache.get(cacheKey)?.let { return Result.success(it) }

        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))

        var bitmap: Bitmap? = null
        val result = client.getCoverArt(id = id, size = sizePx) { inputStream ->
            bitmap = BitmapFactory.decodeStream(inputStream)
        }

        return when (result) {
            is SubsonicResult.Success -> {
                val decoded = bitmap
                    ?: return Result.failure(IllegalStateException("Failed to decode cover art"))
                cache.put(cacheKey, decoded)
                Result.success(decoded)
            }
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    override fun getCacheSizeBytes(): Long = cache.size().toLong()

    override fun clearCache() {
        cache.evictAll()
    }

    private companion object {
        /** 10 MB — sufficient for ~28 album covers at 300×300 ARGB_8888. */
        const val MAX_CACHE_SIZE_BYTES = 10 * 1024 * 1024
    }
}
