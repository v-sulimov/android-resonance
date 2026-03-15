package com.vsulimov.resonance.ui.screen.main.search

/**
 * Complete UI state for the search screen.
 *
 * Combines the persistent search bar state (query, history, filter)
 * with the dynamic result state that changes based on user interaction.
 *
 * @param query Current text in the search bar.
 * @param history Recent search queries, most recent first.
 * @param filter Active result category filter.
 * @param resultState Current state of search results.
 */
data class SearchUiState(
    val query: String = "",
    val history: List<String> = emptyList(),
    val filter: SearchFilter = SearchFilter.ALL,
    val resultState: SearchResultState = SearchResultState.Idle
)

/**
 * State of search results within the search screen.
 *
 * - [Idle]: No search has been performed (query is empty).
 * - [Loading]: A search request is in progress.
 * - [Success]: Results have been received and are ready for display.
 * - [Empty]: Search completed but returned no results.
 * - [Error]: Search request failed with an error message.
 */
sealed class SearchResultState {

    /** No active search. The search history is shown instead. */
    data object Idle : SearchResultState()

    /** A search request is in progress. */
    data object Loading : SearchResultState()

    /**
     * Search completed successfully with results.
     *
     * @param artists Matched artist results.
     * @param albums Matched album results.
     * @param songs Matched song results.
     */
    data class Success(
        val artists: List<SearchArtistViewObject>,
        val albums: List<SearchAlbumViewObject>,
        val songs: List<SearchSongViewObject>
    ) : SearchResultState()

    /** Search completed but returned no matching results. */
    data object Empty : SearchResultState()

    /**
     * Search request failed.
     *
     * @param message Error description for display.
     */
    data class Error(val message: String) : SearchResultState()
}
