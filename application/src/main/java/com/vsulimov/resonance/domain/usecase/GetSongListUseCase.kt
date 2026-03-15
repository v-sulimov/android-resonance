package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.SongSummary
import com.vsulimov.resonance.domain.repository.SongRepository

/**
 * Fetches a paginated list of songs from the library.
 *
 * @param songRepository Repository providing paginated song retrieval.
 */
class GetSongListUseCase(
    private val songRepository: SongRepository
) {

    /**
     * @param size Maximum number of songs per page.
     * @param offset Zero-based index of the first song to return.
     * @return [Result.success] with the song list, or [Result.failure] on error.
     */
    suspend operator fun invoke(
        size: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<SongSummary>> = songRepository.getSongs(size, offset)

    private companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
