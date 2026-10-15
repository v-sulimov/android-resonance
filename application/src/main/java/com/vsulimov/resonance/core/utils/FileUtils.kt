package com.vsulimov.resonance.core.utils

import java.util.Locale

/** Number of bytes in a kilobyte. */
private const val BYTES_PER_KB = 1024.0

/** Number of bytes in a megabyte. */
private const val BYTES_PER_MB = BYTES_PER_KB * 1024.0

/** Number of bytes in a gigabyte. */
private const val BYTES_PER_GB = BYTES_PER_MB * 1024.0

/**
 * Utility functions for formatting file and directory size calculations into human-readable strings.
 */
object FileUtils {

    /**
     * Formats a byte count into a human-readable size string (e.g. "0 B", "512 KB", "12.4 MB", "1.5 GB").
     *
     * @param bytes Size in bytes.
     * @return Formatted size string.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val doubleBytes = bytes.toDouble()
        return when {
            doubleBytes >= BYTES_PER_GB -> String.format(Locale.US, "%.1f GB", doubleBytes / BYTES_PER_GB)
            doubleBytes >= BYTES_PER_MB -> String.format(Locale.US, "%.1f MB", doubleBytes / BYTES_PER_MB)
            doubleBytes >= BYTES_PER_KB -> String.format(Locale.US, "%.1f KB", doubleBytes / BYTES_PER_KB)
            else -> "$bytes B"
        }
    }
}
