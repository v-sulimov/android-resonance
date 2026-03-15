package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.SearchResult
import com.vsulimov.resonance.domain.repository.SearchRepository

/**
 * Searches the music library using the Subsonic `search3` API.
 *
 * Returns grouped results for artists, albums, and songs matching
 * the given query. Result counts are capped at [DEFAULT_RESULT_COUNT]
 * per category.
 *
 * @param searchRepository Repository providing search functionality.
 */
class SearchUseCase(
    private val searchRepository: SearchRepository
) {

    /**
     * Executes a search with the given [query].
     *
     * @param query Search query text. Must not be blank.
     * @return A [Result] containing grouped [SearchResult].
     */
    suspend operator fun invoke(query: String): Result<SearchResult> =
        searchRepository.search(
            query = query,
            artistCount = DEFAULT_RESULT_COUNT,
            albumCount = DEFAULT_RESULT_COUNT,
            songCount = DEFAULT_RESULT_COUNT
        )

    private companion object {
        /** Maximum number of results per category. */
        const val DEFAULT_RESULT_COUNT = 20
    }
}
