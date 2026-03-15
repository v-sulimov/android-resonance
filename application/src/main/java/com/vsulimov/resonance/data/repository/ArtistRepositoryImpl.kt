package com.vsulimov.resonance.data.repository

import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.data.mapper.ArtistSummaryMapper
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.domain.model.ArtistSummary
import com.vsulimov.resonance.domain.repository.ArtistRepository

/**
 * [ArtistRepository] implementation backed by the Subsonic API.
 *
 * Delegates to [SubsonicClient.getArtists][com.vsulimov.libsubsonic.SubsonicClient.getArtists]
 * and maps the response through [ArtistSummaryMapper].
 *
 * @param clientProvider Provides a configured [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] instance.
 */
class ArtistRepositoryImpl(
    private val clientProvider: SubsonicClientProvider
) : ArtistRepository {

    override suspend fun getAllArtists(): Result<List<ArtistSummary>> {
        val client = clientProvider.getClient()
            ?: return Result.failure(IllegalStateException("No authenticated client available"))
        return when (val result = client.getArtists()) {
            is SubsonicResult.Success -> Result.success(
                ArtistSummaryMapper.mapFromIndices(result.data.artists)
            )
            is SubsonicResult.Failure -> Result.failure(result.error)
        }
    }
}
