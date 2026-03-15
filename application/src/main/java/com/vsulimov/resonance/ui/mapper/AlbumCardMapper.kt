package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.AlbumSummary
import com.vsulimov.resonance.ui.screen.main.mix.AlbumCardViewObject

/**
 * Maps domain [AlbumSummary] to presentation [AlbumCardViewObject].
 */
object AlbumCardMapper {

    /**
     * Converts a single [AlbumSummary] into an [AlbumCardViewObject].
     *
     * @param album The domain model to convert.
     * @return A view object ready for UI rendering.
     */
    fun map(album: AlbumSummary): AlbumCardViewObject = AlbumCardViewObject(
        id = album.id,
        name = album.name,
        artistName = album.artistName,
        coverArtId = album.coverArtId
    )

    /**
     * Converts a list of [AlbumSummary] instances into [AlbumCardViewObject] instances.
     *
     * @param albums The domain models to convert.
     * @return A list of view objects ready for UI rendering.
     */
    fun mapList(albums: List<AlbumSummary>): List<AlbumCardViewObject> = albums.map(::map)
}
