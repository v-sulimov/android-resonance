package com.vsulimov.resonance.domain.repository

import com.vsulimov.resonance.domain.model.ArtistSummary

/**
 * Abstraction for retrieving artist data from the music server.
 *
 * All methods return [Result] to allow callers to handle failures gracefully.
 */
interface ArtistRepository {

    /**
     * Returns all artists in the library, sorted alphabetically.
     *
     * The Subsonic API returns artists in a single response (no pagination),
     * grouped by alphabetical index letter. This method flattens the groups
     * into a single ordered list.
     *
     * @return [Result.success] with a list of [ArtistSummary], or
     *   [Result.failure] wrapping the server error.
     */
    suspend fun getAllArtists(): Result<List<ArtistSummary>>
}
