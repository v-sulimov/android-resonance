package com.vsulimov.resonance.data.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.resonance.domain.model.AlbumDetail

/**
 * Maps libsubsonic [Album] (with songs) to the domain [AlbumDetail] model.
 *
 * Used when the full album response from `getAlbum` includes the track list.
 * Delegates track mapping to [TrackSummaryMapper].
 */
object AlbumDetailMapper {

    private const val UNKNOWN_ARTIST = "Unknown Artist"

    /**
     * Converts a full [Album] response into an [AlbumDetail] domain model.
     *
     * @param album The raw album data from the Subsonic API, including songs.
     * @return A domain model with all fields mapped and nulls handled.
     */
    fun map(album: Album): AlbumDetail = AlbumDetail(
        id = album.id,
        name = album.name,
        artistName = album.artist ?: UNKNOWN_ARTIST,
        coverArtId = album.coverArt,
        year = album.year,
        genre = album.genre,
        songCount = album.songCount ?: album.songs.size,
        durationSeconds = album.duration,
        tracks = TrackSummaryMapper.mapList(album.songs)
    )
}
