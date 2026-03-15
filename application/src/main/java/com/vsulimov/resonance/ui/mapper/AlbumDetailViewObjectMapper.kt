package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.AlbumDetail
import com.vsulimov.resonance.ui.screen.albumdetail.AlbumDetailViewObject

/**
 * Maps domain [AlbumDetail] to presentation [AlbumDetailViewObject].
 *
 * Delegates track mapping to [TrackViewObjectMapper]. Metadata fields
 * (year, song count, duration) are preserved as structured data so that
 * the composable layer can format them using localized string resources.
 */
object AlbumDetailViewObjectMapper {

    /**
     * Converts an [AlbumDetail] into an [AlbumDetailViewObject].
     *
     * Structured metadata fields are passed through directly. The composable
     * layer is responsible for building the formatted metadata display line
     * using localized string resources.
     *
     * @param album The domain model to convert.
     * @return A view object ready for UI rendering.
     */
    fun map(album: AlbumDetail): AlbumDetailViewObject = AlbumDetailViewObject(
        id = album.id,
        name = album.name,
        artistName = album.artistName,
        coverArtId = album.coverArtId,
        year = album.year,
        songCount = album.songCount.takeIf { it > 0 },
        durationSeconds = album.durationSeconds,
        tracks = TrackViewObjectMapper.mapList(album.tracks)
    )
}
