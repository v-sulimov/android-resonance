package com.vsulimov.resonance.data.db

import androidx.room.TypeConverter

/**
 * Type converters for Room database mapping custom enums to primitive types.
 */
class Converters {

    /**
     * Converts string representation back to [DownloadStatus] enum.
     *
     * @param value Raw status string stored in DB.
     * @return Corresponding [DownloadStatus] enum or default [DownloadStatus.NOT_DOWNLOADED].
     */
    @TypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus =
        value?.let { runCatching { DownloadStatus.valueOf(it) }.getOrNull() }
            ?: DownloadStatus.NOT_DOWNLOADED

    /**
     * Converts [DownloadStatus] enum to its name string.
     *
     * @param status Enum instance.
     * @return String name of the enum.
     */
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String = status?.name ?: DownloadStatus.NOT_DOWNLOADED.name
}
