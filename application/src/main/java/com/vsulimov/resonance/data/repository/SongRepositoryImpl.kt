package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.mapper.SongSummaryMapper
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.model.SongSummary
import com.vsulimov.resonance.domain.repository.SongRepository

/**
 * [SongRepository] implementation backed by the Subsonic `search3` API.
 *
 * Uses an empty query with `artistCount=0` and `albumCount=0` to fetch
 * only songs, paginated via [songCount] and [songOffset].
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class SongRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : SongRepository {

    override suspend fun getSongs(size: Int, offset: Int): Result<List<SongSummary>> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))

        return when (
            val result = client.search(
                query = "",
                artistCount = 0,
                artistOffset = 0,
                albumCount = 0,
                albumOffset = 0,
                songCount = size,
                songOffset = offset
            )
        ) {
            is SubsonicResult.Success -> {
                val songs = SongSummaryMapper.mapList(result.data.songs)
                Result.success(songs)
            }
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }
}
