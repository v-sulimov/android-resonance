package com.vsulimov.resonance.ui.screens.downloads

import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import com.vsulimov.resonance.data.db.entity.OfflineAlbumEntity
import com.vsulimov.resonance.data.db.entity.OfflineArtistEntity
import com.vsulimov.resonance.data.db.entity.OfflinePlaylistEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity

/**
 * Filter tabs available on the Downloads & Storage Management Screen.
 */
enum class DownloadedTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}

/** Sorting criteria options available for downloaded content management. */
enum class DownloadSortCriteria {
    SIZE_DESC,
    NAME_ASC,
    SONG_COUNT
}

/** Data item pairing a downloaded album entity with precalculated track count and byte size. */
data class DownloadedAlbumItem(val album: OfflineAlbumEntity, val songCount: Int, val totalBytes: Long)

/** Data item pairing a downloaded artist entity with precalculated album count, track count, and byte size. */
data class DownloadedArtistItem(
    val artist: OfflineArtistEntity,
    val albumCount: Int,
    val songCount: Int,
    val totalBytes: Long
)

/** Data item pairing a downloaded playlist entity with precalculated track count and byte size. */
data class DownloadedPlaylistItem(val playlist: OfflinePlaylistEntity, val songCount: Int, val totalBytes: Long)

/**
 * Target item selected for offline storage deletion.
 */
sealed interface DeleteTarget {
    /** Target song item for deletion. */
    data class Song(val song: OfflineSongEntity) : DeleteTarget

    /** Target album collection for deletion. */
    data class Album(val item: DownloadedAlbumItem) : DeleteTarget

    /** Target artist discography for deletion. */
    data class Artist(val item: DownloadedArtistItem) : DeleteTarget

    /** Target playlist collection for deletion. */
    data class Playlist(val item: DownloadedPlaylistItem) : DeleteTarget
}

/**
 * Data holder pairing a download task entity with its associated offline song metadata entity if present.
 *
 * @property task The download task entity.
 * @property song Associated song entity or null if unavailable.
 */
data class DownloadTaskWithSong(val task: DownloadTaskEntity, val song: OfflineSongEntity?)

/**
 * Immutable UI State model for the Downloads & Storage Management Screen.
 *
 * @property isOfflineMode Whether strict offline mode toggle is active.
 * @property isQueuePaused Whether the active download queue worker is currently paused.
 * @property audioStorageBytes Bytes used by offline audio tracks.
 * @property coversStorageBytes Bytes used by permanent offline cover art.
 * @property totalOfflineStorageBytes Combined offline storage usage in bytes.
 * @property freeDiskBytes Usable free internal disk space in bytes.
 * @property totalDiskBytes Total internal storage capacity in bytes.
 * @property activeTaskIds List of unique song IDs for currently active download tasks.
 * @property downloadedSongs List of downloaded [OfflineSongEntity] models.
 * @property downloadedAlbums List of downloaded [DownloadedAlbumItem] models.
 * @property downloadedArtists List of downloaded [DownloadedArtistItem] models.
 * @property downloadedPlaylists List of downloaded [DownloadedPlaylistItem] models.
 * @property selectedTab Currently active filter tab.
 * @property selectedSortCriteria Currently active sort criteria option.
 * @property searchQuery Active search filter query string.
 * @property expandedArtistIds Set of expanded artist IDs for drill-down hierarchy.
 * @property expandedAlbumIds Set of expanded album IDs for drill-down hierarchy.
 * @property isLibraryFullyDownloaded Whether the entire server library is already downloaded.
 * @property isDownloadLibraryDialogOpen Whether confirmation dialog for downloading entire library is open.
 * @property isRemoveAllDialogOpen Whether confirmation dialog for removing all downloads is open.
 * @property pendingDeleteTarget Target item selected for deletion confirmation or null.
 */
data class DownloadsUiState(
    val isOfflineMode: Boolean = false,
    val isQueuePaused: Boolean = false,
    val audioStorageBytes: Long = 0L,
    val coversStorageBytes: Long = 0L,
    val totalOfflineStorageBytes: Long = 0L,
    val freeDiskBytes: Long = 0L,
    val totalDiskBytes: Long = 0L,
    val activeTaskIds: List<String> = emptyList(),
    val downloadedSongs: List<OfflineSongEntity> = emptyList(),
    val downloadedAlbums: List<DownloadedAlbumItem> = emptyList(),
    val downloadedArtists: List<DownloadedArtistItem> = emptyList(),
    val downloadedPlaylists: List<DownloadedPlaylistItem> = emptyList(),
    val selectedTab: DownloadedTab = DownloadedTab.ARTISTS,
    val selectedSortCriteria: DownloadSortCriteria = DownloadSortCriteria.SIZE_DESC,
    val searchQuery: String = "",
    val expandedArtistIds: Set<String> = emptySet(),
    val expandedAlbumIds: Set<String> = emptySet(),
    val isLibraryFullyDownloaded: Boolean = false,
    val isDownloadLibraryDialogOpen: Boolean = false,
    val isRemoveAllDialogOpen: Boolean = false,
    val pendingDeleteTarget: DeleteTarget? = null
)
