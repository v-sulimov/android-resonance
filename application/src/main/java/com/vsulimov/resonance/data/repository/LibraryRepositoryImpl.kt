package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.data.response.lists.AlbumListType
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.repository.LibraryRepository

/**
 * [LibraryRepository] implementation backed by the Subsonic API.
 *
 * Fetches library-wide category counts by querying the appropriate
 * server endpoints and aggregating the results.
 *
 * Album and song counts are obtained by paginating through the full
 * album list, which avoids the inaccuracies of deriving counts from
 * artist or genre aggregates (multi-artist albums inflate artist-based
 * counts; untagged songs are missed by genre-based counts).
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class LibraryRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : LibraryRepository {

    override suspend fun getArtistCount(): Result<Int> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getArtists()) {
            is SubsonicResult.Success -> {
                val count = result.data.artists.sumOf { it.artists.size }
                Result.success(count)
            }
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    override suspend fun getAlbumAndSongCounts(): Result<Pair<Int, Int>> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))

        var albumCount = 0
        var songCount = 0
        var offset = 0

        while (true) {
            when (
                val result = client.getAlbumList(
                    type = AlbumListType.ALPHABETICAL_BY_NAME,
                    size = PAGE_SIZE,
                    offset = offset
                )
            ) {
                is SubsonicResult.Success -> {
                    val albums = result.data.albums
                    albumCount += albums.size
                    songCount += albums.sumOf { it.songCount ?: 0 }
                    if (albums.size < PAGE_SIZE) break
                    offset += PAGE_SIZE
                }
                is SubsonicResult.Failure -> return Result.failure(result.error)
            }
        }

        return Result.success(albumCount to songCount)
    }

    override suspend fun getGenreCount(): Result<Int> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getGenres()) {
            is SubsonicResult.Success -> Result.success(result.data.genres.size)
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    override suspend fun getPlaylistCount(): Result<Int> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getPlaylists()) {
            is SubsonicResult.Success -> Result.success(result.data.playlists.size)
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    override suspend fun getFavoriteTrackCount(): Result<Int> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getStarred()) {
            is SubsonicResult.Success -> Result.success(result.data.songs.size)
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    private companion object {
        /** Maximum albums per page when paginating the album list. */
        const val PAGE_SIZE = 500
    }
}
