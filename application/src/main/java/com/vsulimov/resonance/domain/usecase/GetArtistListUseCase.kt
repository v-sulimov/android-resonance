package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.ArtistSummary
import com.vsulimov.resonance.domain.repository.ArtistRepository

/**
 * Fetches all artists from the library.
 *
 * The Subsonic API returns all artists in a single response, so no
 * pagination is needed.
 *
 * @param artistRepository Repository providing artist data from the server.
 */
class GetArtistListUseCase(
    private val artistRepository: ArtistRepository
) {

    /**
     * Returns all artists sorted alphabetically.
     *
     * @return [Result.success] with a list of [ArtistSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend operator fun invoke(): Result<List<ArtistSummary>> =
        artistRepository.getAllArtists()
}
