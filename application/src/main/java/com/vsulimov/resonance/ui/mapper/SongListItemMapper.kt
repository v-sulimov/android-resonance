package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.SongSummary
import com.vsulimov.resonance.ui.screen.songlist.SongListItemViewObject

/**
 * Maps domain [SongSummary] to presentation [SongListItemViewObject].
 */
object SongListItemMapper {

    /**
     * Converts a single [SongSummary] to a [SongListItemViewObject].
     */
    fun map(song: SongSummary): SongListItemViewObject = SongListItemViewObject(
        id = song.id,
        title = song.title,
        artistName = song.artistName,
        albumName = song.albumName,
        albumId = song.albumId,
        coverArtId = song.coverArtId,
        durationSeconds = song.durationSeconds
    )

    /**
     * Converts a list of [SongSummary] to [SongListItemViewObject] instances.
     */
    fun mapList(songs: List<SongSummary>): List<SongListItemViewObject> = songs.map(::map)
}
