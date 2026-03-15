package com.vsulimov.resonance.domain.usecase

import com.vsulimov.resonance.domain.model.LibraryCounts
import com.vsulimov.resonance.domain.repository.LibraryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches all library category counts needed by the Library tab in parallel.
 *
 * Issues five concurrent requests via [LibraryRepository] and assembles
 * the results into a [LibraryCounts] instance. Each count is an independent
 * [Result] so that individual failures do not block the entire screen.
 *
 * Album and song counts share a single paginated request (album list),
 * so only five logical operations produce six category counts.
 *
 * @param libraryRepository Repository providing library count operations.
 */
class GetLibraryCountsUseCase(
    private val libraryRepository: LibraryRepository
) {

    /**
     * Fetches artist, album, song, playlist, genre, and favorite track
     * counts in parallel.
     *
     * @return [LibraryCounts] containing independent results for each category.
     */
    suspend operator fun invoke(): LibraryCounts = coroutineScope {
        val artist = async { libraryRepository.getArtistCount() }
        val albumSong = async { libraryRepository.getAlbumAndSongCounts() }
        val genre = async { libraryRepository.getGenreCount() }
        val playlist = async { libraryRepository.getPlaylistCount() }
        val favorite = async { libraryRepository.getFavoriteTrackCount() }

        val albumSongResult = albumSong.await()

        LibraryCounts(
            artistCount = artist.await(),
            albumCount = albumSongResult.map { it.first },
            songCount = albumSongResult.map { it.second },
            genreCount = genre.await(),
            playlistCount = playlist.await(),
            favoriteTrackCount = favorite.await()
        )
    }
}
