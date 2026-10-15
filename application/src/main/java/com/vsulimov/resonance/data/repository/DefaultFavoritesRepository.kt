package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Implementation of [FavoritesRepository] using [SubsonicClient].
 *
 * @property settingsRepository Optional [SettingsRepository] to check for offline mode status.
 * @property getSubsonicClient Provider function to get the current authenticated [SubsonicClient].
 */
class DefaultFavoritesRepository(
    private val settingsRepository: SettingsRepository? = null,
    private val getSubsonicClient: () -> SubsonicClient?
) : FavoritesRepository {

    private val _starredSongIds = MutableStateFlow<Set<String>>(emptySet())
    override val starredSongIds: StateFlow<Set<String>> = _starredSongIds.asStateFlow()

    private val _starredAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    override val starredAlbumIds: StateFlow<Set<String>> = _starredAlbumIds.asStateFlow()

    private val _starredArtistIds = MutableStateFlow<Set<String>>(emptySet())
    override val starredArtistIds: StateFlow<Set<String>> = _starredArtistIds.asStateFlow()

    override suspend fun loadFavorites(): Result<Unit> = withContext(Dispatchers.IO) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) {
            return@withContext Result.success(Unit)
        }
        val client =
            getSubsonicClient() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        when (val result = client.getStarred()) {
            is SubsonicResult.Success -> {
                val songs = result.data.songs.mapTo(HashSet()) { it.id }
                val albums = result.data.albums.mapTo(HashSet()) { it.id }
                val artists = result.data.artists.mapTo(HashSet()) { it.id }

                _starredSongIds.value = songs
                _starredAlbumIds.value = albums
                _starredArtistIds.value = artists
                Result.success(Unit)
            }

            is SubsonicResult.Failure -> {
                Result.failure(Exception(result.error.toString()))
            }
        }
    }

    override suspend fun toggleSongFavorite(songId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) {
            return@withContext Result.failure(IllegalStateException("Favorite actions are disabled in offline mode"))
        }
        val client =
            getSubsonicClient() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val currentlyStarred = _starredSongIds.value.contains(songId)
        val newStarred = !currentlyStarred

        _starredSongIds.update { if (newStarred) it + songId else it - songId }

        val result = if (newStarred) {
            client.star(ids = listOf(songId))
        } else {
            client.unstar(ids = listOf(songId))
        }

        when (result) {
            is SubsonicResult.Success -> Result.success(newStarred)

            is SubsonicResult.Failure -> {
                _starredSongIds.update { if (currentlyStarred) it + songId else it - songId }
                Result.failure(Exception(result.error.toString()))
            }
        }
    }

    override suspend fun toggleAlbumFavorite(albumId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) {
            return@withContext Result.failure(IllegalStateException("Favorite actions are disabled in offline mode"))
        }
        val client =
            getSubsonicClient() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val currentlyStarred = _starredAlbumIds.value.contains(albumId)
        val newStarred = !currentlyStarred

        _starredAlbumIds.update { if (newStarred) it + albumId else it - albumId }

        val result = if (newStarred) {
            client.star(albumIds = listOf(albumId))
        } else {
            client.unstar(albumIds = listOf(albumId))
        }

        when (result) {
            is SubsonicResult.Success -> Result.success(newStarred)

            is SubsonicResult.Failure -> {
                _starredAlbumIds.update { if (currentlyStarred) it + albumId else it - albumId }
                Result.failure(Exception(result.error.toString()))
            }
        }
    }

    override suspend fun toggleArtistFavorite(artistId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (settingsRepository?.isOfflineModeEnabledSync() == true) {
            return@withContext Result.failure(IllegalStateException("Favorite actions are disabled in offline mode"))
        }
        val client =
            getSubsonicClient() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val currentlyStarred = _starredArtistIds.value.contains(artistId)
        val newStarred = !currentlyStarred

        _starredArtistIds.update { if (newStarred) it + artistId else it - artistId }

        val result = if (newStarred) {
            client.star(artistIds = listOf(artistId))
        } else {
            client.unstar(artistIds = listOf(artistId))
        }

        when (result) {
            is SubsonicResult.Success -> Result.success(newStarred)

            is SubsonicResult.Failure -> {
                _starredArtistIds.update { if (currentlyStarred) it + artistId else it - artistId }
                Result.failure(Exception(result.error.toString()))
            }
        }
    }

    override fun isSongStarred(songId: String): Boolean = _starredSongIds.value.contains(songId)

    override fun isAlbumStarred(albumId: String): Boolean = _starredAlbumIds.value.contains(albumId)

    override fun isArtistStarred(artistId: String): Boolean = _starredArtistIds.value.contains(artistId)

    override fun registerStarredSong(songId: String) {
        _starredSongIds.update { it + songId }
    }

    override fun registerStarredAlbum(albumId: String) {
        _starredAlbumIds.update { it + albumId }
    }

    override fun registerStarredArtist(artistId: String) {
        _starredArtistIds.update { it + artistId }
    }

    override fun clearFavorites() {
        _starredSongIds.value = emptySet()
        _starredAlbumIds.value = emptySet()
        _starredArtistIds.value = emptySet()
    }
}
