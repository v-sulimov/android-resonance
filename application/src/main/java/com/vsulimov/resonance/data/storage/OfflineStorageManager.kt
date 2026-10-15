package com.vsulimov.resonance.data.storage

import android.content.Context
import android.graphics.Bitmap
import com.vsulimov.resonance.core.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val BUFFER_SIZE = 128 * 1024 // 128 KB buffer for fast audio streaming
private const val TEMP_EXTENSION = ".tmp"

/**
 * Manages permanent local storage for offline audio files and cover art.
 *
 * Responsibilities:
 * - Isolation: Audio files stored in `filesDir/offline_audio/`, cover art in `filesDir/offline_covers/`.
 * - Atomic Writes: Downloads/writes occur in temporary `.tmp` files and are atomically renamed on success.
 * - Disk Space Management: Queries available and used disk space.
 */
class OfflineStorageManager(context: Context) {

    private val appContext = context.applicationContext

    /** Directory storing permanent downloaded audio tracks. */
    val offlineAudioDir: File = File(appContext.filesDir, "offline_audio").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /** Directory storing permanent offline cover art images. */
    val offlineCoversDir: File = File(appContext.filesDir, "offline_covers").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    init {
        cleanupTempFiles()
    }

    /**
     * Resolves the target file for a downloaded audio song.
     *
     * @param songId Unique song ID.
     * @param suffix Optional file extension (e.g. "mp3", "flac").
     * @return [File] pointing to target path.
     */
    fun getAudioFile(songId: String, suffix: String? = null): File {
        if (!suffix.isNullOrBlank()) {
            val exactFile = File(offlineAudioDir, buildFileName(songId, suffix))
            if (exactFile.exists() && exactFile.length() > 0) {
                return exactFile
            }
        }
        val existing = offlineAudioDir.listFiles { _, name ->
            name.startsWith("$songId.") && !name.endsWith(TEMP_EXTENSION)
        }?.firstOrNull { it.exists() && it.length() > 0 }
        if (existing != null) {
            return existing
        }
        return File(offlineAudioDir, buildFileName(songId, suffix))
    }

    /**
     * Checks whether an offline audio file exists on disk.
     *
     * @param songId Unique song ID.
     * @param suffix Optional file extension.
     * @return True if target file exists and is non-empty.
     */
    fun hasAudioFile(songId: String, suffix: String? = null): Boolean {
        if (!suffix.isNullOrBlank()) {
            val exactFile = File(offlineAudioDir, buildFileName(songId, suffix))
            if (exactFile.exists() && exactFile.length() > 0) {
                return true
            }
        }
        val existing = offlineAudioDir.listFiles { _, name ->
            name.startsWith("$songId.") && !name.endsWith(TEMP_EXTENSION)
        }?.firstOrNull { it.exists() && it.length() > 0 }
        return existing != null
    }

    /**
     * Atomically saves an audio [InputStream] to disk.
     * Writes to a temporary `.tmp` file and renames to final target upon completion.
     *
     * @param songId Unique song ID.
     * @param suffix Optional file extension.
     * @param inputStream Input stream containing raw audio data.
     * @param onProgress Optional callback reporting total bytes written.
     * @return The finalized [File] on success.
     */
    suspend fun saveAudioStream(
        songId: String,
        suffix: String?,
        inputStream: InputStream,
        onProgress: (suspend (bytesWritten: Long) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val finalFile = getAudioFile(songId, suffix)
        val tempFile = File(offlineAudioDir, "${finalFile.name}$TEMP_EXTENSION")

        try {
            var totalWritten = 0L
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    ensureActive()
                    outputStream.write(buffer, 0, bytesRead)
                    totalWritten += bytesRead
                    onProgress?.invoke(totalWritten)
                }
                outputStream.flush()
            }

            if (!tempFile.renameTo(finalFile)) {
                /* Fallback atomic copy if rename fails */
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            return@withContext finalFile
        } catch (t: Throwable) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (t is kotlinx.coroutines.CancellationException) throw t
            throw t
        }
    }

    /**
     * Deletes a downloaded audio file from local storage.
     *
     * @param songId Unique song ID.
     * @param suffix Optional file extension.
     * @return True if file was successfully deleted or didn't exist.
     */
    fun deleteAudioFile(songId: String, suffix: String? = null): Boolean {
        val file = getAudioFile(songId, suffix)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Resolves the file for an offline cover art item using a hashed cache key.
     *
     * @param coverArtId Cover art unique ID.
     * @return [File] pointing to cover file location in offline_covers folder.
     */
    fun getOfflineCoverFile(coverArtId: String): File {
        val cacheKey = hashString(coverArtId)
        return File(offlineCoversDir, cacheKey)
    }

    /**
     * Checks if permanent offline cover art exists for given cover art ID.
     *
     * @param coverArtId Cover art unique ID.
     * @return True if file exists and is non-empty.
     */
    fun hasOfflineCover(coverArtId: String): Boolean {
        val file = getOfflineCoverFile(coverArtId)
        return file.exists() && file.length() > 0
    }

    /**
     * Atomically saves a cover art [InputStream] to permanent offline covers directory.
     *
     * @param coverArtId Cover art unique ID.
     * @param inputStream Stream containing image data.
     * @return Finalized cover [File].
     */
    suspend fun saveOfflineCoverStream(coverArtId: String, inputStream: InputStream): File =
        withContext(Dispatchers.IO) {
            val finalFile = getOfflineCoverFile(coverArtId)
            val tempFile = File(offlineCoversDir, "${finalFile.name}$TEMP_EXTENSION")

            try {
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream, BUFFER_SIZE)
                    outputStream.flush()
                }

                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                return@withContext finalFile
            } catch (t: Throwable) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                if (t is kotlinx.coroutines.CancellationException) throw t
                throw t
            }
        }

    /**
     * Saves a cover art [Bitmap] directly to permanent offline covers directory.
     *
     * @param coverArtId Cover art unique ID.
     * @param bitmap Bitmap image to persist.
     * @return Finalized cover [File].
     */
    suspend fun saveOfflineCoverBitmap(coverArtId: String, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val finalFile = getOfflineCoverFile(coverArtId)
        val tempFile = File(offlineCoversDir, "${finalFile.name}$TEMP_EXTENSION")

        try {
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.DISK_CACHE_QUALITY, outputStream)
                outputStream.flush()
            }

            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            return@withContext finalFile
        } catch (t: Throwable) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (t is kotlinx.coroutines.CancellationException) throw t
            throw t
        }
    }

    /**
     * Deletes a permanent offline cover art file.
     *
     * @param coverArtId Cover art unique ID.
     * @return True if deleted or non-existent.
     */
    fun deleteOfflineCover(coverArtId: String): Boolean {
        val file = getOfflineCoverFile(coverArtId)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Calculates total bytes consumed by downloaded audio tracks.
     *
     * @return Total size in bytes.
     */
    fun getAudioStorageSizeBytes(): Long = offlineAudioDir.listFiles()?.sumOf { it.length() } ?: 0L

    /**
     * Calculates total bytes consumed by permanent offline cover art files.
     *
     * @return Total size in bytes.
     */
    fun getCoversStorageSizeBytes(): Long = offlineCoversDir.listFiles()?.sumOf { it.length() } ?: 0L

    /**
     * Calculates total bytes consumed by all offline content (audio + covers).
     *
     * @return Combined size in bytes.
     */
    fun getTotalOfflineStorageSizeBytes(): Long = getAudioStorageSizeBytes() + getCoversStorageSizeBytes()

    /**
     * Returns usable free disk space on internal storage in bytes.
     *
     * @return Free space in bytes.
     */
    fun getFreeDiskSizeBytes(): Long = appContext.filesDir.usableSpace

    /**
     * Returns total internal storage capacity in bytes.
     *
     * @return Total capacity in bytes.
     */
    fun getTotalDiskSizeBytes(): Long = appContext.filesDir.totalSpace

    /**
     * Deletes all downloaded audio files and permanent offline covers.
     */
    fun clearAllOfflineStorage() {
        offlineAudioDir.listFiles()?.forEach { it.delete() }
        offlineCoversDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Constructs filename for audio file combining song ID and extension.
     */
    private fun buildFileName(songId: String, suffix: String?): String {
        val ext = suffix?.trim()?.removePrefix(".")?.takeIf { it.isNotBlank() } ?: "mp3"
        return "$songId.$ext"
    }

    /**
     * Creates an MD5 hash of a string to ensure safe, unique filenames.
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance(Constants.ALGORITHM_MD5).digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Clears orphaned temporary files from previous interrupted downloads.
     */
    private fun cleanupTempFiles() {
        offlineAudioDir.listFiles { _, name -> name.endsWith(TEMP_EXTENSION) }?.forEach { it.delete() }
        offlineCoversDir.listFiles { _, name -> name.endsWith(TEMP_EXTENSION) }?.forEach { it.delete() }
    }
}
