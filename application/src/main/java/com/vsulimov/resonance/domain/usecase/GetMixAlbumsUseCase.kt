package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.domain.repository.AlbumRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Aggregated result of all Mix tab album list requests.
 *
 * Each field is an independent [Result] so that a failure in one
 * carousel does not prevent the others from displaying. The presentation
 * layer filters out failed results and shows whichever carousels succeed.
 *
 * @property recentlyPlayed Albums the user played most recently.
 * @property mostPlayed Most frequently played albums.
 * @property recentlyAdded Newest albums added to the library.
 * @property randomPicks Random album selection.
 */
data class MixAlbums(
    val recentlyPlayed: Result<List<AlbumSummary>>,
    val mostPlayed: Result<List<AlbumSummary>>,
    val recentlyAdded: Result<List<AlbumSummary>>,
    val randomPicks: Result<List<AlbumSummary>>
)

/**
 * Fetches all four album lists needed by the Mix tab in parallel.
 *
 * Uses [coroutineScope] with [async] to issue all four requests
 * concurrently, minimizing total load time. Individual failures
 * are captured in the corresponding [Result] within [MixAlbums].
 *
 * @param albumRepository Repository providing album list operations.
 */
class GetMixAlbumsUseCase(
    private val albumRepository: AlbumRepository
) {

    /**
     * Fetches recently played, most played, recently added, and random
     * album lists in parallel.
     *
     * @param count Maximum number of albums per list.
     * @return [MixAlbums] containing independent results for each list.
     */
    suspend operator fun invoke(count: Int = DEFAULT_ALBUM_COUNT): MixAlbums = coroutineScope {
        val recentlyPlayed = async { albumRepository.getRecentlyPlayed(count) }
        val mostPlayed = async { albumRepository.getMostPlayed(count) }
        val recentlyAdded = async { albumRepository.getRecentlyAdded(count) }
        val randomPicks = async { albumRepository.getRandomPicks(count) }

        MixAlbums(
            recentlyPlayed = recentlyPlayed.await(),
            mostPlayed = mostPlayed.await(),
            recentlyAdded = recentlyAdded.await(),
            randomPicks = randomPicks.await()
        )
    }

    private companion object {
        const val DEFAULT_ALBUM_COUNT = 7
    }
}
