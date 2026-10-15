package com.vsulimov.resonance.core.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Maximum allowed size for the ephemeral disk cache directory (100 MB). */
private const val MAX_DISK_CACHE_SIZE_BYTES = 100L * 1024L * 1024L

/** Suffix appended to incomplete temporary files during download to prevent partial reads. */
private const val TEMP_EXTENSION = ".tmp"

/**
 * A custom, lightweight image loader specifically designed for Subsonic cover art.
 *
 * Features:
 * - Multi-level caching: In-memory [LruCache], permanent offline storage (`filesDir/offline_covers/`), and ephemeral disk cache (`cacheDir/covers/`).
 * - Background execution on [Dispatchers.IO].
 * - Uses [SubsonicClient.getCoverArt] to fetch image streams.
 * - Thread-safe per-key locking with automatic cleanup to prevent memory leaks.
 * - Automatic LRU disk cache trimming for ephemeral cache to bound disk space usage.
 */
class SubsonicImageLoader(
    context: Context,
    private val offlineStorageManager: OfflineStorageManager? = null,
    private val getClient: () -> SubsonicClient?
) {

    private val appContext = context.applicationContext

    /**
     * In-memory cache.
     * Max size is calculated based on 1/8th of the available max memory.
     */
    private val memoryCache: LruCache<String, Bitmap>

    /** Directory where disk-cached images are stored. */
    private val diskCacheDir: File = File(appContext.cacheDir, "covers").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /** Mutexes per cache key to prevent concurrent network downloads for the same image. */
    private val loadingMutexes = ConcurrentHashMap<String, Mutex>()

    /** Synchronization lock object for coordinating concurrent writes to the disk cache. */
    private val diskCacheLock = Any()

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
     * 2. Check permanent offline covers (`filesDir/offline_covers/`).
     * 3. Check ephemeral disk cache (`cacheDir/covers/`).
     * 4. Download from network via [SubsonicClient].
     *
     * @param coverArtId The ID of the cover art from the Subsonic API.
     * @param size An optional size hint for the server (e.g., to request a thumbnail).
     * @return The loaded [Bitmap], or null if loading fails or the client is not configured.
     */
    suspend fun loadCoverArt(coverArtId: String, size: Int? = null): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(coverArtId, size)

        memoryCache[cacheKey]?.let { return@withContext it }

        val mutex = loadingMutexes.getOrPut(cacheKey) { Mutex() }

        try {
            mutex.withLock {
                memoryCache[cacheKey]?.let { return@withContext it }

                offlineStorageManager?.let { storage ->
                    val offlineCoverFile = storage.getOfflineCoverFile(coverArtId)
                    if (offlineCoverFile.exists() && offlineCoverFile.length() > 0) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(offlineCoverFile.absolutePath)
                            if (bitmap != null) {
                                memoryCache.put(cacheKey, bitmap)
                                return@withContext bitmap
                            }
                        } catch (t: Throwable) {
                            if (t is kotlinx.coroutines.CancellationException) throw t
                        }
                    }
                }

                val diskFile = File(diskCacheDir, cacheKey)
                if (diskFile.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                        if (bitmap != null) {
                            memoryCache.put(cacheKey, bitmap)
                            return@withContext bitmap
                        }
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                    }
                }

                val client = getClient() ?: return@withContext null

                var loadedBitmap: Bitmap? = null
                try {
                    client.getCoverArt(id = coverArtId, size = size) { inputStream ->
                        inputStream.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                memoryCache.put(cacheKey, bitmap)
                                saveToDiskCache(diskFile, bitmap)
                                loadedBitmap = bitmap
                            }
                        }
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                }

                return@withContext loadedBitmap
            }
        } finally {
            loadingMutexes.remove(cacheKey)
        }
    }

    /**
     * Persists a cover art image permanently into offline covers storage.
     *
     * @param coverArtId Unique cover art ID.
     * @param bitmap Bitmap image to save.
     */
    suspend fun savePermanentOfflineCover(coverArtId: String, bitmap: Bitmap) {
        offlineStorageManager?.saveOfflineCoverBitmap(coverArtId, bitmap)
    }

    /**
     * Attempts to fetch an image synchronously from the in-memory cache.
     * This prevents 1-frame UI flashes for images already in memory.
     */
    fun getFromMemoryCache(coverArtId: String, size: Int? = null): Bitmap? =
        memoryCache[generateCacheKey(coverArtId, size)]

    /**
     * Calculates the total size of disk-cached cover art files in bytes.
     *
     * @return Total size in bytes.
     */
    fun getCacheSizeBytes(): Long = diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    /**
     * Clears both the in-memory cache and the disk cache folder.
     */
    fun clearCache() {
        memoryCache.evictAll()
        loadingMutexes.clear()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Saves a bitmap to the specified disk file atomically and trims disk cache if limit is reached.
     */
    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        synchronized(diskCacheLock) {
            try {
                val tempFile = File(diskCacheDir, "${file.name}$TEMP_EXTENSION")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.DISK_CACHE_QUALITY, out)
                }
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
                trimDiskCacheIfNeeded()
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
            }
        }
    }

    /**
     * Trims oldest disk cache files if total disk cache size exceeds [MAX_DISK_CACHE_SIZE_BYTES].
     * Caller must hold diskCacheLock.
     */
    private fun trimDiskCacheIfNeeded() {
        val files = diskCacheDir.listFiles()?.filter { !it.name.endsWith(TEMP_EXTENSION) } ?: return
        var currentSize = files.sumOf { it.length() }

        if (currentSize > MAX_DISK_CACHE_SIZE_BYTES) {
            val sortedFiles = files.sortedBy { it.lastModified() }
            for (f in sortedFiles) {
                if (currentSize <= MAX_DISK_CACHE_SIZE_BYTES) break
                val length = f.length()
                if (f.delete()) {
                    currentSize -= length
                }
            }
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
