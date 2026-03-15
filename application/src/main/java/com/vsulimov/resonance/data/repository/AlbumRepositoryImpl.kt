package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.data.response.lists.AlbumListType
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.mapper.AlbumDetailMapper
import com.vsulimov.resonance.data.mapper.AlbumSummaryMapper
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.model.AlbumDetail
import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.domain.repository.AlbumRepository

/**
 * [AlbumRepository] implementation backed by the Subsonic API.
 *
 * Each method delegates to [SubsonicClient.getAlbumList][com.vsulimov.libsubsonic.SubsonicClient.getAlbumList]
 * with the appropriate [AlbumListType] and maps the response through
 * [AlbumSummaryMapper].
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class AlbumRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : AlbumRepository {

    override suspend fun getAlbumDetail(id: String): Result<AlbumDetail> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getAlbum(id = id)) {
            is SubsonicResult.Success -> Result.success(
                AlbumDetailMapper.map(result.data.album)
            )
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    override suspend fun getRecentlyPlayed(count: Int): Result<List<AlbumSummary>> =
        fetchAlbums(AlbumListType.RECENT, count)

    override suspend fun getMostPlayed(count: Int): Result<List<AlbumSummary>> =
        fetchAlbums(AlbumListType.FREQUENT, count)

    override suspend fun getRecentlyAdded(count: Int): Result<List<AlbumSummary>> =
        fetchAlbums(AlbumListType.NEWEST, count)

    override suspend fun getRandomPicks(count: Int): Result<List<AlbumSummary>> =
        fetchAlbums(AlbumListType.RANDOM, count)

    override suspend fun getAlbums(
        sortType: AlbumSortType,
        size: Int,
        offset: Int
    ): Result<List<AlbumSummary>> {
        val type = when (sortType) {
            AlbumSortType.ALPHABETICAL -> AlbumListType.ALPHABETICAL_BY_NAME
            AlbumSortType.RECENTLY_PLAYED -> AlbumListType.RECENT
            AlbumSortType.MOST_PLAYED -> AlbumListType.FREQUENT
            AlbumSortType.RECENTLY_ADDED -> AlbumListType.NEWEST
        }
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getAlbumList(type = type, size = size, offset = offset)) {
            is SubsonicResult.Success -> Result.success(
                AlbumSummaryMapper.mapList(result.data.albums)
            )
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }

    /**
     * Fetches an album list of the given [type] and maps it to domain models.
     *
     * @param type The list type determining which albums the server returns.
     * @param count Maximum number of albums to request.
     * @return [Result.success] with mapped [AlbumSummary] list, or
     *   [Result.failure] wrapping the error.
     */
    private suspend fun fetchAlbums(
        type: AlbumListType,
        count: Int
    ): Result<List<AlbumSummary>> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getAlbumList(type = type, size = count)) {
            is SubsonicResult.Success -> Result.success(
                AlbumSummaryMapper.mapList(result.data.albums)
            )
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }
}
