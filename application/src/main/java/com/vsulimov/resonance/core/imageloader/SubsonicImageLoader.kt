package com.vsulimov.resonance.core.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.resonance.core.Constants
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A custom, lightweight image loader specifically designed for Subsonic cover art.
 *
 * Features:
 * - Two-level caching: In-memory [LruCache] and disk cache ([Context.cacheDir]).
 * - Background execution on [Dispatchers.IO].
 * - Uses [SubsonicClient.getCoverArt] to fetch image streams.
 */
class SubsonicImageLoader(context: Context, private val getClient: () -> SubsonicClient?) {
    /**
     * In-memory cache.
     * Max size is calculated based on 1/8th of the available max memory.
     */
    private val memoryCache: LruCache<String, Bitmap>

    /** Directory where disk-cached images are stored. */
    private val diskCacheDir: File = File(context.cacheDir, "covers").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / Constants.KILOBYTE).toInt()
        val cacheSize = maxMemory / Constants.MEMORY_CACHE_DIVISOR

        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / Constants.KILOBYTE
        }
    }

    /**
     * Loads a cover art bitmap for the given [coverArtId].
     *
     * The loading process follows this priority:
     * 1. Check in-memory [LruCache].
     * 2. Check disk cache (`cacheDir/covers/`).
     * 3. Download from network via [SubsonicClient].
     *
     * @param coverArtId The ID of the cover art from the Subsonic API.
     * @param size An optional size hint for the server (e.g., to request a thumbnail).
     * @return The loaded [Bitmap], or null if loading fails or the client is not configured.
     */
    suspend fun loadCoverArt(coverArtId: String, size: Int? = null): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(coverArtId, size)

        memoryCache.get(cacheKey)?.let { return@withContext it }

        val diskFile = File(diskCacheDir, cacheKey)
        if (diskFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
                return@withContext bitmap
            }
        }

        val client = getClient() ?: return@withContext null

        var loadedBitmap: Bitmap? = null
        try {
            client.getCoverArt(id = coverArtId, size = size) { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    saveToDiskCache(diskFile, bitmap)
                    loadedBitmap = bitmap
                }
            }
        } catch (_: Exception) {
            // Loading failed, return null to show a placeholder
        }

        return@withContext loadedBitmap
    }

    /**
     * Clears both the in-memory cache and the disk cache folder.
     */
    fun clearCache() {
        memoryCache.evictAll()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Saves a bitmap to the specified disk file.
     */
    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.DISK_CACHE_QUALITY, out)
            }
        } catch (_: Exception) {
            // If disk saving fails, we still have it in memory
        }
    }

    /**
     * Generates a safe, unique filename for a cover art item based on its ID and requested size.
     */
    private fun generateCacheKey(id: String, size: Int?): String {
        val rawKey = if (size != null) "${id}_$size" else id
        return hashString(rawKey)
    }

    /**
     * Creates an MD5 hash of a string to ensure it's safe to use as a filename.
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance(Constants.ALGORITHM_MD5).digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
