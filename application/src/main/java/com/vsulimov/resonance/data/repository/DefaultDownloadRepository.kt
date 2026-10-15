package com.vsulimov.resonance.data.repository

import android.content.Context
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.response.lists.AlbumListType
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.db.ResonanceDatabase
import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import com.vsulimov.resonance.data.db.mapper.toOfflineEntity
import com.vsulimov.resonance.data.download.DownloadForegroundService
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.data.storage.OfflineStorageManager
import com.vsulimov.resonance.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LIBRARY_PAGINATION_SIZE = 100

/**
 * Default implementation of [DownloadRepository].
 * Resolves metadata from Subsonic server, persists entities into Room database, and starts [DownloadForegroundService].
 */
class DefaultDownloadRepository(
    private val context: Context,
    private val database: ResonanceDatabase,
    private val offlineStorageManager: OfflineStorageManager,
    private val settingsRepository: SettingsRepository,
    private val getClient: () -> SubsonicClient?
) : DownloadRepository {

    override fun resumeActiveDownloadsOnAppStart() {
        CoroutineScope(Dispatchers.IO).launch {
            val isPaused = settingsRepository.isDownloadQueuePausedState.value
            val activeTasks = database.downloadTaskDao().getTasksByStatus(DownloadStatus.QUEUED) +
                database.downloadTaskDao().getTasksByStatus(DownloadStatus.DOWNLOADING)
            if (activeTasks.isNotEmpty() && !isPaused) {
                DownloadForegroundService.start(context)
            }
        }
    }

    override suspend fun enqueueSong(song: Child) {
        withContext(Dispatchers.IO) {
            settingsRepository.setDownloadQueuePaused(false)
            val existing = database.songDao().getSongById(song.id)
            val songEntity = song.toOfflineEntity(
                localFilePath = existing?.localFilePath,
                downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
            )
            database.songDao().insertSong(songEntity)
            enqueueTaskForSong(songEntity)
            DownloadForegroundService.start(context)
        }
    }

    override suspend fun enqueueAlbum(albumId: String) {
        withContext(Dispatchers.IO) {
            val client = getClient() ?: return@withContext
            val result = client.getAlbum(albumId)
            if (result is SubsonicResult.Success) {
                settingsRepository.setDownloadQueuePaused(false)
                val album = result.data.album
                database.albumDao().insertAlbum(album.toOfflineEntity())
                val songEntities = album.songs.map { child ->
                    val existing = database.songDao().getSongById(child.id)
                    child.toOfflineEntity(
                        localFilePath = existing?.localFilePath,
                        downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
                    )
                }
                database.songDao().insertSongs(songEntities)
                songEntities.forEach { enqueueTaskForSong(it) }
                DownloadForegroundService.start(context)
            }
        }
    }

    override suspend fun enqueueArtist(artistId: String) {
        withContext(Dispatchers.IO) {
            val client = getClient() ?: return@withContext
            val artistResult = client.getArtist(artistId)
            if (artistResult is SubsonicResult.Success) {
                settingsRepository.setDownloadQueuePaused(false)
                val artist = artistResult.data.artist
                database.artistDao().insertArtist(artist.toOfflineEntity())

                artist.albums.forEach { albumSummary ->
                    val albumResult = client.getAlbum(albumSummary.id)
                    if (albumResult is SubsonicResult.Success) {
                        val fullAlbum = albumResult.data.album
                        database.albumDao().insertAlbum(fullAlbum.toOfflineEntity())
                        val songEntities = fullAlbum.songs.map { child ->
                            val existing = database.songDao().getSongById(child.id)
                            child.toOfflineEntity(
                                localFilePath = existing?.localFilePath,
                                downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
                            )
                        }
                        database.songDao().insertSongs(songEntities)
                        songEntities.forEach { enqueueTaskForSong(it) }
                    }
                }
                DownloadForegroundService.start(context)
            }
        }
    }

    override suspend fun enqueuePlaylist(playlistId: String) {
        withContext(Dispatchers.IO) {
            val client = getClient() ?: return@withContext
            val playlistResult = client.getPlaylist(playlistId)
            if (playlistResult is SubsonicResult.Success) {
                settingsRepository.setDownloadQueuePaused(false)
                val playlist = playlistResult.data.playlist
                val songEntities = playlist.entries.map { child ->
                    val existing = database.songDao().getSongById(child.id)
                    child.toOfflineEntity(
                        localFilePath = existing?.localFilePath,
                        downloadedAt = existing?.downloadedAt ?: System.currentTimeMillis()
                    )
                }
                database.songDao().insertSongs(songEntities)
                database.playlistDao().updatePlaylistWithSongs(
                    playlist = playlist.toOfflineEntity(),
                    songs = songEntities
                )
                songEntities.forEach { enqueueTaskForSong(it) }
                DownloadForegroundService.start(context)
            }
        }
    }

    override suspend fun enqueueEntireLibrary() {
        withContext(Dispatchers.IO) {
            val client = getClient() ?: return@withContext
            settingsRepository.setDownloadQueuePaused(false)
            var offset = 0

            while (true) {
                val albumListResult = client.getAlbumList(
                    type = AlbumListType.ALPHABETICAL_BY_NAME,
                    size = LIBRARY_PAGINATION_SIZE,
                    offset = offset
                )

                if (albumListResult !is SubsonicResult.Success) break
                val albums = albumListResult.data.albums
                if (albums.isEmpty()) break

                albums.forEach { albumSummary ->
                    val fullAlbumResult = client.getAlbum(albumSummary.id)
                    if (fullAlbumResult is SubsonicResult.Success) {
                        val fullAlbum = fullAlbumResult.data.album
                        database.albumDao().insertAlbum(fullAlbum.toOfflineEntity())
                        val songEntities = fullAlbum.songs.map { it.toOfflineEntity() }
                        database.songDao().insertSongs(songEntities)
                        songEntities.forEach { enqueueTaskForSong(it) }
                    }
                }

                if (albums.size < LIBRARY_PAGINATION_SIZE) break
                offset += LIBRARY_PAGINATION_SIZE
            }
            DownloadForegroundService.start(context)
        }
    }

    override suspend fun cancelDownload(songId: String) {
        withContext(Dispatchers.IO) {
            deleteDownloadedSong(songId)
        }
    }

    override suspend fun cancelAllDownloads() {
        withContext(Dispatchers.IO) {
            val queuedAndActive = database.downloadTaskDao().getTasksByStatus(DownloadStatus.QUEUED) +
                database.downloadTaskDao().getTasksByStatus(DownloadStatus.DOWNLOADING)
            queuedAndActive.forEach { task ->
                deleteDownloadedSong(task.songId)
            }
            DownloadForegroundService.cancelAll(context)
        }
    }

    override suspend fun clearFailedDownloads() {
        withContext(Dispatchers.IO) {
            database.downloadTaskDao().deleteTasksByStatus(DownloadStatus.FAILED)
        }
    }

    override suspend fun deleteDownloadedSong(songId: String) {
        withContext(Dispatchers.IO) {
            val song = database.songDao().getSongById(songId)
            song?.let {
                offlineStorageManager.deleteAudioFile(it.id, it.suffix)
                database.downloadTaskDao().deleteTaskById(it.id)
                database.songDao().deleteSongById(it.id)
                cleanupOrphanedCover(it.coverArt)

                val albumId = it.albumId
                if (albumId != null) {
                    val remainingAlbumSongs = database.songDao().getSongsByAlbumId(albumId)
                    if (remainingAlbumSongs.isEmpty()) {
                        val album = database.albumDao().getAlbumById(albumId)
                        database.albumDao().deleteAlbumById(albumId)
                        cleanupOrphanedCover(album?.coverArt)
                    }
                }

                val artistId = it.artistId
                if (artistId != null) {
                    val remainingArtistSongs = database.songDao().getSongsByArtistId(artistId)
                    if (remainingArtistSongs.isEmpty()) {
                        val artist = database.artistDao().getArtistById(artistId)
                        database.artistDao().deleteArtistById(artistId)
                        cleanupOrphanedCover(artist?.coverArt)
                    }
                }
            }
        }
    }

    override suspend fun deleteDownloadedAlbum(albumId: String) {
        withContext(Dispatchers.IO) {
            val album = database.albumDao().getAlbumById(albumId)
            val songsByAlbumId = database.songDao().getSongsByAlbumId(albumId)
            val songsByAlbumName =
                album?.name?.let { database.songDao().getSongsByAlbumIdOrName(albumId, it) } ?: emptyList()
            val allSongs = (songsByAlbumId + songsByAlbumName).distinctBy { it.id }

            val songs = if (allSongs.isNotEmpty()) {
                allSongs
            } else {
                val cleanName = albumId.removePrefix("name_")
                database.songDao().getSongsByAlbumIdOrName(albumId, cleanName)
            }

            val artistId = songs.firstOrNull()?.artistId
            database.albumDao().deleteAlbumById(albumId)
            cleanupOrphanedCover(album?.coverArt)

            songs.forEach { song ->
                offlineStorageManager.deleteAudioFile(song.id, song.suffix)
                database.downloadTaskDao().deleteTaskById(song.id)

                val inPlaylist = database.songDao().getPlaylistCountForSong(song.id) > 0
                if (inPlaylist) {
                    database.songDao().insertSong(song.copy(localFilePath = null))
                } else {
                    database.songDao().deleteSongById(song.id)
                }
                cleanupOrphanedCover(song.coverArt)
            }

            if (artistId != null) {
                val remainingArtistSongs = database.songDao().getSongsByArtistId(artistId)
                if (remainingArtistSongs.isEmpty()) {
                    val artist = database.artistDao().getArtistById(artistId)
                    database.artistDao().deleteArtistById(artistId)
                    cleanupOrphanedCover(artist?.coverArt)
                }
            }
        }
    }

    override suspend fun deleteDownloadedArtist(artistId: String) {
        withContext(Dispatchers.IO) {
            val songs = database.songDao().getSongsByArtistId(artistId)
            songs.forEach { song ->
                offlineStorageManager.deleteAudioFile(song.id, song.suffix)
                database.downloadTaskDao().deleteTaskById(song.id)

                val inPlaylist = database.songDao().getPlaylistCountForSong(song.id) > 0
                if (inPlaylist) {
                    database.songDao().insertSong(song.copy(localFilePath = null))
                } else {
                    database.songDao().deleteSongById(song.id)
                }
                cleanupOrphanedCover(song.coverArt)
            }
            val artist = database.artistDao().getArtistById(artistId)
            database.artistDao().deleteArtistById(artistId)
            cleanupOrphanedCover(artist?.coverArt)

            val albums = database.albumDao().getAlbumsByArtistId(artistId)
            albums.forEach { album ->
                val remainingSongs = database.songDao().getSongsByAlbumId(album.id)
                if (remainingSongs.isEmpty()) {
                    database.albumDao().deleteAlbumById(album.id)
                    cleanupOrphanedCover(album.coverArt)
                }
            }
        }
    }

    override suspend fun deleteDownloadedPlaylist(playlistId: String) {
        withContext(Dispatchers.IO) {
            val songs = database.playlistDao().getSongsForPlaylist(playlistId)

            songs.forEach { song ->
                val inOtherPlaylist = database.songDao().getPlaylistCountForSong(song.id) > 1
                val inAlbum = song.albumId != null && database.albumDao().getAlbumById(song.albumId) != null

                if (!inOtherPlaylist && !inAlbum) {
                    offlineStorageManager.deleteAudioFile(song.id, song.suffix)
                    database.downloadTaskDao().deleteTaskById(song.id)
                    database.songDao().insertSong(song.copy(localFilePath = null))
                }
            }
        }
    }

    override suspend fun deleteAllDownloads() {
        withContext(Dispatchers.IO) {
            DownloadForegroundService.cancelAll(context)
            offlineStorageManager.clearAllOfflineStorage()
            database.downloadTaskDao().clearAll()
            database.songDao().clearAll()
            database.albumDao().clearAll()
            database.artistDao().clearAll()
            database.playlistDao().clearAll()
        }
    }

    override fun observeAllTasks(): Flow<List<DownloadTaskEntity>> = database.downloadTaskDao().observeAllTasks()

    override fun observeAllTasksSimplified(): Flow<List<DownloadTaskEntity>> =
        database.downloadTaskDao().observeAllTasks()
            .map { tasks ->
                tasks.map { it.copy(bytesDownloaded = 0L, updatedAt = 0L) }
            }
            .distinctUntilChanged()

    override fun observeTaskBySongId(songId: String): Flow<DownloadTaskEntity?> =
        database.downloadTaskDao().observeTaskBySongId(songId)

    override fun observeActiveTaskCount(): Flow<Int> = database.downloadTaskDao().observeAllTasks().map { tasks ->
        tasks.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
    }

    private suspend fun cleanupOrphanedCover(coverArtId: String?) {
        if (coverArtId.isNullOrBlank()) return
        val isUsed = database.songDao().countSongsWithCoverArt(coverArtId) > 0 ||
            database.albumDao().countAlbumsWithCoverArt(coverArtId) > 0 ||
            database.artistDao().countArtistsWithCoverArt(coverArtId) > 0 ||
            database.playlistDao().countPlaylistsWithCoverArt(coverArtId) > 0
        if (!isUsed) {
            offlineStorageManager.deleteOfflineCover(coverArtId)
        }
    }

    private suspend fun enqueueTaskForSong(song: OfflineSongEntity) {
        val existingTask = database.downloadTaskDao().getTaskBySongId(song.id)
        if (existingTask == null || existingTask.status == DownloadStatus.FAILED ||
            existingTask.status == DownloadStatus.NOT_DOWNLOADED
        ) {
            val now = System.currentTimeMillis()
            val task = DownloadTaskEntity(
                id = song.id,
                songId = song.id,
                status = DownloadStatus.QUEUED,
                bytesDownloaded = 0L,
                totalBytes = song.size ?: 0L,
                errorMessage = null,
                addedAt = now,
                updatedAt = now
            )
            database.downloadTaskDao().insertTask(task)
        }
    }
}
