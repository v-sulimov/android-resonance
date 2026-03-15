package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.AlbumDetail
import com.vsulimov.resonance.domain.repository.AlbumRepository

/**
 * Retrieves full album details including the track list.
 *
 * Used by the album detail screen to load all information
 * needed to display album metadata and its track listing.
 *
 * @param albumRepository Repository providing album data from the server.
 */
class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository
) {

    /**
     * Fetches album details for the given identifier.
     *
     * @param albumId Unique album identifier.
     * @return [Result.success] with an [AlbumDetail], or
     *   [Result.failure] wrapping the server error.
     */
    suspend operator fun invoke(albumId: String): Result<AlbumDetail> =
        albumRepository.getAlbumDetail(albumId)
}
