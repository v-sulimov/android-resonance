package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.mapper.ArtistSummaryMapper
import com.vsulimov.resonance.data.mapper.SearchAlbumMapper
import com.vsulimov.resonance.data.mapper.SongSummaryMapper
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.model.SearchResult
import com.vsulimov.resonance.domain.repository.SearchRepository

/**
 * [SearchRepository] implementation backed by the Subsonic `search3` API.
 *
 * Maps the raw API response into domain models using dedicated mappers
 * for each result type (artists, albums, songs).
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class SearchRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : SearchRepository {

    override suspend fun search(
        query: String,
        artistCount: Int,
        albumCount: Int,
        songCount: Int
    ): Result<SearchResult> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))

        return when (
            val result = client.search(
                query = query,
                artistCount = artistCount,
                artistOffset = 0,
                albumCount = albumCount,
                albumOffset = 0,
                songCount = songCount,
                songOffset = 0
            )
        ) {
            is SubsonicResult.Success -> {
                val searchResult = SearchResult(
                    artists = result.data.artists.map(ArtistSummaryMapper::map),
                    albums = SearchAlbumMapper.mapList(result.data.albums),
                    songs = SongSummaryMapper.mapList(result.data.songs)
                )
                Result.success(searchResult)
            }
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }
}
