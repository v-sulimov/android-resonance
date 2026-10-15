package com.vsulimov.resonance.ui.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.ui.getAppContainer
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.db.entity.OfflineAlbumEntity
import com.vsulimov.resonance.data.db.entity.OfflineArtistEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import com.vsulimov.resonance.data.download.DownloadForegroundService
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import com.vsulimov.resonance.domain.repository.DownloadRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STORAGE_REFRESH_THROTTLE_MS = 1000L

/**
 * ViewModel responsible for managing storage usage statistics, active download tasks, and offline content management.
 */
class DownloadsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val offlineStorageManager: OfflineStorageManager,
    private val downloadRepository: DownloadRepository,
    private val offlineRepository: OfflineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())

    /** Exposes the immutable UI state for the Downloads Screen. */
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private var lastStorageRefreshTime = 0L

    init {
        observeSettings()
        observeDownloadTasks()
        observeDownloadedContent()
        checkIsLibraryFullyDownloaded()
        refreshStorageInfo(force = true)
        syncPlaylistsGraph()
    }

    private fun syncPlaylistsGraph() {
        if (!settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                offlineRepository.syncPlaylistsGraph {
                    context.getAppContainer().getAuthenticatedSubsonicClient()
                }
            }
        }
    }

    /**
     * Updates the selected tab filter for downloaded content management.
     *
     * @param tab Selected [DownloadedTab].
     */
    fun onTabSelected(tab: DownloadedTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Updates selected sort criteria option for downloaded content.
     *
     * @param criteria Selected [DownloadSortCriteria].
     */
    fun onSortCriteriaSelected(criteria: DownloadSortCriteria) {
        _uiState.update { it.copy(selectedSortCriteria = criteria) }
    }

    /**
     * Updates search query string for filtering downloaded items.
     *
     * @param query Search string entered by user.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Toggles expansion state for an artist row in drill-down hierarchy.
     *
     * @param artistId Unique artist ID.
     */
    fun toggleArtistExpanded(artistId: String) {
        _uiState.update { state ->
            val updated = state.expandedArtistIds.toMutableSet()
            if (!updated.add(artistId)) {
                updated.remove(artistId)
            }
            state.copy(expandedArtistIds = updated)
        }
    }

    /**
     * Toggles expansion state for an album row in drill-down hierarchy.
     *
     * @param albumId Unique album ID.
     */
    fun toggleAlbumExpanded(albumId: String) {
        _uiState.update { state ->
            val updated = state.expandedAlbumIds.toMutableSet()
            if (!updated.add(albumId)) {
                updated.remove(albumId)
            }
            state.copy(expandedAlbumIds = updated)
        }
    }

    /**
     * Toggles visibility of Remove All Downloads confirmation dialog.
     *
     * @param open True to show dialog.
     */
    fun setRemoveAllDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isRemoveAllDialogOpen = open) }
    }

    /**
     * Deletes all downloaded audio files, permanent offline covers, and clears Room offline metadata.
     */
    fun removeAllDownloads() {
        setRemoveAllDialogOpen(false)
        viewModelScope.launch {
            downloadRepository.deleteAllDownloads()
            refreshStorageInfo(force = true)
        }
    }

    /**
     * Sets target item selected for deletion or null to dismiss dialog.
     *
     * @param target Selected [DeleteTarget] or null.
     */
    fun setPendingDeleteTarget(target: DeleteTarget?) {
        _uiState.update { it.copy(pendingDeleteTarget = target) }
    }

    /**
     * Confirms and executes deletion of the currently pending delete target.
     */
    fun confirmPendingDelete() {
        val target = _uiState.value.pendingDeleteTarget ?: return
        _uiState.update { it.copy(pendingDeleteTarget = null) }

        viewModelScope.launch {
            when (target) {
                is DeleteTarget.Song -> downloadRepository.deleteDownloadedSong(target.song.id)
                is DeleteTarget.Album -> downloadRepository.deleteDownloadedAlbum(target.item.album.id)
                is DeleteTarget.Artist -> downloadRepository.deleteDownloadedArtist(target.item.artist.id)
                is DeleteTarget.Playlist -> downloadRepository.deleteDownloadedPlaylist(target.item.playlist.id)
            }
            refreshStorageInfo(force = true)
        }
    }

    /**
     * Toggles strict offline mode.
     *
     * @param enabled True to activate offline mode.
     */
    fun toggleOfflineMode(enabled: Boolean) {
        settingsRepository.setOfflineModeEnabled(enabled)
    }

    /**
     * Toggles visibility of confirmation dialog for downloading entire library.
     *
     * @param open True to show dialog.
     */
    fun setDownloadLibraryDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isDownloadLibraryDialogOpen = open) }
    }

    /**
     * Triggers batch download of the entire server music library.
     */
    fun downloadEntireLibrary() {
        setDownloadLibraryDialogOpen(false)
        viewModelScope.launch {
            downloadRepository.enqueueEntireLibrary()
        }
    }

    /**
     * Pauses active download worker service.
     */
    fun pauseDownloads() {
        settingsRepository.setDownloadQueuePaused(true)
        _uiState.update { it.copy(isQueuePaused = true) }
        DownloadForegroundService.pause(context)
    }

    /**
     * Resumes active download worker service.
     */
    fun resumeDownloads() {
        settingsRepository.setDownloadQueuePaused(false)
        _uiState.update { it.copy(isQueuePaused = false) }
        DownloadForegroundService.start(context)
    }

    /**
     * Cancels a specific download task and deletes partial file.
     *
     * @param songId Unique song ID.
     */
    fun cancelDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(songId)
            refreshStorageInfo(force = true)
        }
    }

    /**
     * Cancels all queued and active download tasks and removes them.
     */
    fun cancelAllDownloads() {
        viewModelScope.launch {
            downloadRepository.cancelAllDownloads()
            refreshStorageInfo(force = true)
        }
    }

    /**
     * Clears all failed download tasks from the database queue.
     */
    fun clearFailedDownloads() {
        viewModelScope.launch {
            downloadRepository.clearFailedDownloads()
            refreshStorageInfo(force = true)
        }
    }

    /**
     * Refreshes internal and offline storage usage byte statistics with throttling.
     *
     * @param force True to bypass throttling and force immediate refresh.
     */
    fun refreshStorageInfo(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastStorageRefreshTime < STORAGE_REFRESH_THROTTLE_MS) {
            return
        }
        lastStorageRefreshTime = now

        viewModelScope.launch(Dispatchers.IO) {
            val audioBytes = offlineStorageManager.getAudioStorageSizeBytes()
            val coversBytes = offlineStorageManager.getCoversStorageSizeBytes()
            val totalBytes = offlineStorageManager.getTotalOfflineStorageSizeBytes()
            val freeBytes = offlineStorageManager.getFreeDiskSizeBytes()
            val totalDisk = offlineStorageManager.getTotalDiskSizeBytes()

            _uiState.update {
                it.copy(
                    audioStorageBytes = audioBytes,
                    coversStorageBytes = coversBytes,
                    totalOfflineStorageBytes = totalBytes,
                    freeDiskBytes = freeBytes,
                    totalDiskBytes = totalDisk
                )
            }
        }
    }

    /**
     * Observes the download task and song metadata for a given song ID.
     *
     * @param songId Target song ID.
     * @return [Flow] emitting paired task and song metadata.
     */
    fun observeTaskWithSong(songId: String): Flow<DownloadTaskWithSong?> {
        val db = context.getAppContainer().database
        return combine(
            db.downloadTaskDao().observeTaskBySongId(songId),
            db.songDao().observeSongById(songId)
        ) { task, song ->
            if (task == null) null else DownloadTaskWithSong(task, song)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.isOfflineModeEnabledState.collect { enabled ->
                _uiState.update { it.copy(isOfflineMode = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isDownloadQueuePausedState.collect { paused ->
                _uiState.update { it.copy(isQueuePaused = paused) }
            }
        }
    }

    private fun observeDownloadTasks() {
        viewModelScope.launch {
            downloadRepository.observeAllTasks()
                .map { tasks ->
                    tasks.filter {
                        it.status == DownloadStatus.QUEUED ||
                            it.status == DownloadStatus.DOWNLOADING ||
                            it.status == DownloadStatus.FAILED
                    }.map { it.songId }
                }
                .distinctUntilChanged()
                .collect { activeIds ->
                    _uiState.update {
                        it.copy(activeTaskIds = activeIds)
                    }
                    refreshStorageInfo(force = false)
                }
        }
    }

    private fun observeDownloadedContent() {
        val db = context.getAppContainer().database
        val songDao = db.songDao()
        val albumDao = db.albumDao()
        val artistDao = db.artistDao()
        val playlistDao = db.playlistDao()

        val downloadedSongIdsFlow = downloadRepository.observeAllTasks()
            .map { tasks ->
                tasks.filter { it.status == DownloadStatus.DOWNLOADED }.map { it.songId }.toSet()
            }
            .distinctUntilChanged()

        viewModelScope.launch {
            combine(
                downloadedSongIdsFlow,
                songDao.observeAllSongs(),
                albumDao.observeAllAlbums(),
                artistDao.observeAllArtists(),
                playlistDao.observeAllPlaylists()
            ) { downloadedSongIds, songs, albums, artists, playlists ->

                val downloadedSongs = songs.filter { song ->
                    downloadedSongIds.contains(song.id) || song.localFilePath != null
                }

                // Map of known albums from Room
                val albumMap = albums.associateBy { it.id }.toMutableMap()

                // Group downloaded songs by album
                val songsGroupedByAlbumKey = downloadedSongs.groupBy { song ->
                    song.albumId ?: "name_${song.album?.trim()?.lowercase() ?: "unknown"}"
                }

                // Ensure every album represented in downloadedSongs has an OfflineAlbumEntity
                songsGroupedByAlbumKey.forEach { (albumKey, albumSongs) ->
                    val sampleSong = albumSongs.firstOrNull() ?: return@forEach
                    val albumName = sampleSong.album ?: "Unknown Album"

                    if (!albumMap.containsKey(albumKey) &&
                        albumMap.values.none { it.name.equals(albumName, ignoreCase = true) }
                    ) {
                        albumMap[albumKey] = OfflineAlbumEntity(
                            id = sampleSong.albumId ?: albumKey,
                            name = albumName,
                            artist = sampleSong.artist,
                            artistId = sampleSong.artistId,
                            coverArt = sampleSong.coverArt,
                            songCount = albumSongs.size,
                            duration = albumSongs.sumOf { it.duration },
                            created = sampleSong.downloadedAt.toString(),
                            year = null,
                            genre = null,
                            isStarred = false
                        )
                    }
                }

                val downloadedAlbumItems = albumMap.values.mapNotNull { albumEntity ->
                    val albumSongs = downloadedSongs.filter { song ->
                        (song.albumId != null && song.albumId == albumEntity.id) ||
                            (song.album != null && song.album.equals(albumEntity.name, ignoreCase = true))
                    }
                    if (albumSongs.isNotEmpty()) {
                        val count = albumSongs.size
                        val totalBytes = albumSongs.sumOf { it.size ?: 0L }
                        DownloadedAlbumItem(album = albumEntity, songCount = count, totalBytes = totalBytes)
                    } else {
                        null
                    }
                }

                // Map of known artists from Room
                val artistMap = artists.associateBy { it.id }.toMutableMap()

                downloadedAlbumItems.forEach { item ->
                    val artistName = item.album.artist
                    val artistId = item.album.artistId
                    if (!artistName.isNullOrBlank()) {
                        val key = artistId ?: "name_${artistName.trim().lowercase()}"
                        if (!artistMap.containsKey(key) &&
                            artistMap.values.none { it.name.equals(artistName, ignoreCase = true) }
                        ) {
                            artistMap[key] = OfflineArtistEntity(
                                id = artistId ?: key,
                                name = artistName,
                                coverArt = item.album.coverArt,
                                albumCount = 1,
                                isStarred = false
                            )
                        }
                    }
                }

                downloadedSongs.forEach { song ->
                    val artistName = song.artist
                    val artistId = song.artistId
                    if (!artistName.isNullOrBlank()) {
                        val key = artistId ?: "name_${artistName.trim().lowercase()}"
                        if (!artistMap.containsKey(key) &&
                            artistMap.values.none { it.name.equals(artistName, ignoreCase = true) }
                        ) {
                            artistMap[key] = OfflineArtistEntity(
                                id = artistId ?: key,
                                name = artistName,
                                coverArt = song.coverArt,
                                albumCount = 1,
                                isStarred = false
                            )
                        }
                    }
                }

                val downloadedArtistItems = artistMap.values.mapNotNull { artistEntity ->
                    val artistAlbums = downloadedAlbumItems.filter { item ->
                        item.album.artistId == artistEntity.id ||
                            (
                                item.album.artist != null && item.album.artist.equals(
                                    artistEntity.name,
                                    ignoreCase = true
                                )
                                )
                    }
                    val artistSongs = downloadedSongs.filter { song ->
                        song.artistId == artistEntity.id ||
                            (song.artist != null && song.artist.equals(artistEntity.name, ignoreCase = true)) ||
                            artistAlbums.any { it.album.id == song.albumId }
                    }

                    if (artistAlbums.isNotEmpty() || artistSongs.isNotEmpty()) {
                        val albumCount = maxOf(artistAlbums.size, 1)
                        val songCount = maxOf(artistSongs.size, artistAlbums.sumOf { it.songCount })
                        val totalBytes = if (artistSongs.isNotEmpty()) {
                            artistSongs.sumOf { it.size ?: 0L }
                        } else {
                            artistAlbums.sumOf { it.totalBytes }
                        }
                        DownloadedArtistItem(
                            artist = artistEntity,
                            albumCount = albumCount,
                            songCount = songCount,
                            totalBytes = totalBytes
                        )
                    } else {
                        null
                    }
                }

                val downloadedPlaylistItems = playlists.mapNotNull { playlist ->
                    val songIds = playlistDao.getSongIdsForPlaylist(playlist.id)
                    if (songIds.isEmpty() || playlist.songCount <= 0) return@mapNotNull null

                    val downloadedSongsInPlaylist = songIds.mapNotNull { songId ->
                        downloadedSongs.firstOrNull { it.id == songId }
                    }

                    val isFullyDownloaded = songIds.size >= playlist.songCount &&
                        downloadedSongsInPlaylist.size >= playlist.songCount

                    if (isFullyDownloaded) {
                        val count = downloadedSongsInPlaylist.size
                        val totalBytes = downloadedSongsInPlaylist.sumOf { it.size ?: 0L }
                        DownloadedPlaylistItem(playlist = playlist, songCount = count, totalBytes = totalBytes)
                    } else {
                        null
                    }
                }

                DownloadedContentData(
                    songs = downloadedSongs,
                    albums = downloadedAlbumItems,
                    artists = downloadedArtistItems,
                    playlists = downloadedPlaylistItems
                )
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        downloadedSongs = data.songs,
                        downloadedAlbums = data.albums,
                        downloadedArtists = data.artists,
                        downloadedPlaylists = data.playlists
                    )
                }
            }
        }
    }

    private fun checkIsLibraryFullyDownloaded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = context.getAppContainer().getAuthenticatedSubsonicClient() ?: return@launch
                val scanResult = client.getScanStatus()
                if (scanResult is SubsonicResult.Success) {
                    val serverCount = scanResult.data.scanStatus.count
                    if (serverCount != null && serverCount > 0) {
                        val downloadedCount = context.getAppContainer().database.songDao()
                            .getAllDownloadedSongIds().size
                        if (downloadedCount >= serverCount) {
                            _uiState.update { it.copy(isLibraryFullyDownloaded = true) }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ResonanceApplication
                DownloadsViewModel(
                    context = app,
                    settingsRepository = appContainer.settingsRepository,
                    offlineStorageManager = appContainer.offlineStorageManager,
                    downloadRepository = appContainer.downloadRepository,
                    offlineRepository = appContainer.offlineRepository
                )
            }
        }
    }
}

private data class DownloadedContentData(
    val songs: List<OfflineSongEntity>,
    val albums: List<DownloadedAlbumItem>,
    val artists: List<DownloadedArtistItem>,
    val playlists: List<DownloadedPlaylistItem>
)
