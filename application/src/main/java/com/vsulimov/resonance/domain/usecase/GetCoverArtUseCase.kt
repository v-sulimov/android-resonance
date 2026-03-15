package com.vsulimov.resonance.domain.usecase

import android.graphics.Bitmap
import com.vsulimov.resonance.domain.repository.CoverArtRepository

/**
 * Retrieves cover art for a given identifier.
 *
 * Delegates to [CoverArtRepository] which handles in-memory caching
 * and network fetching. Used by the presentation layer to load album
 * thumbnails in carousel cards.
 *
 * @param coverArtRepository Repository providing cached cover art retrieval.
 */
class GetCoverArtUseCase(
    private val coverArtRepository: CoverArtRepository
) {

    /**
     * Fetches the cover art image for the given [id].
     *
     * @param id Cover art identifier from the album metadata.
     * @param sizePx Requested image dimensions in pixels.
     * @return [Result.success] with a decoded [Bitmap], or
     *   [Result.failure] if retrieval or decoding failed.
     */
    suspend operator fun invoke(id: String, sizePx: Int): Result<Bitmap> =
        coverArtRepository.getCoverArt(id, sizePx)
}
