package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.domain.repository.AlbumRepository

/**
 * Fetches a fresh set of random albums for the Mix tab's "Random picks" carousel.
 *
 * Separated from [GetMixAlbumsUseCase] because refreshing random picks
 * is a standalone user action (tapping "Refresh") that should not
 * reload the other three carousels.
 *
 * @param albumRepository Repository providing album list operations.
 */
class RefreshRandomPicksUseCase(
    private val albumRepository: AlbumRepository
) {

    /**
     * Fetches a new random selection of albums.
     *
     * @param count Maximum number of albums to return.
     * @return [Result.success] with random [AlbumSummary] list, or
     *   [Result.failure] wrapping the server error.
     */
    suspend operator fun invoke(count: Int = DEFAULT_ALBUM_COUNT): Result<List<AlbumSummary>> =
        albumRepository.getRandomPicks(count)

    private companion object {
        const val DEFAULT_ALBUM_COUNT = 7
    }
}
