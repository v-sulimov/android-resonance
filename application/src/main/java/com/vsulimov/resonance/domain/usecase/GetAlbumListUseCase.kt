package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.AlbumSortType
import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.domain.repository.AlbumRepository

/**
 * Fetches a paginated list of albums with the given sort/filter type.
 *
 * Used by the album list screen to load albums in pages as the
 * user scrolls through the grid.
 *
 * @param albumRepository Repository providing album data from the server.
 */
class GetAlbumListUseCase(
    private val albumRepository: AlbumRepository
) {

    /**
     * Returns a page of albums with the given sort type.
     *
     * @param sortType Determines the ordering applied by the server.
     * @param size Maximum number of albums to return.
     * @param offset Zero-based offset for pagination.
     * @return [Result.success] with a list of [AlbumSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend operator fun invoke(
        sortType: AlbumSortType,
        size: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<AlbumSummary>> = albumRepository.getAlbums(sortType, size, offset)

    private companion object {
        const val DEFAULT_PAGE_SIZE = 40
    }
}
