package com.vsulimov.resonance.ui.mapper

import com.vsulimov.resonance.domain.model.ArtistSummary
import com.vsulimov.resonance.domain.model.SearchAlbum
import com.vsulimov.resonance.domain.model.SearchResult
import com.vsulimov.resonance.domain.model.SongSummary
import com.vsulimov.resonance.ui.screen.main.search.SearchAlbumViewObject
import com.vsulimov.resonance.ui.screen.main.search.SearchArtistViewObject
import com.vsulimov.resonance.ui.screen.main.search.SearchSongViewObject

/**
 * Maps domain [SearchResult] models to presentation-layer view objects.
 *
 * Pure mapping functions with no Android dependencies, keeping
 * the presentation layer testable.
 */
object SearchResultMapper {

    /**
     * Maps a domain [ArtistSummary] to a [SearchArtistViewObject].
     */
    fun mapArtist(artist: ArtistSummary): SearchArtistViewObject = SearchArtistViewObject(
        id = artist.id,
        name = artist.name,
        albumCount = artist.albumCount,
        coverArtId = artist.coverArtId
    )

    /**
     * Maps a domain [SearchAlbum] to a [SearchAlbumViewObject].
     */
    fun mapAlbum(album: SearchAlbum): SearchAlbumViewObject = SearchAlbumViewObject(
        id = album.id,
        name = album.name,
        artistName = album.artistName,
        coverArtId = album.coverArtId,
        songCount = album.songCount,
        year = album.year
    )

    /**
     * Maps a domain [SongSummary] to a [SearchSongViewObject].
     */
    fun mapSong(song: SongSummary): SearchSongViewObject = SearchSongViewObject(
        id = song.id,
        title = song.title,
        artistName = song.artistName,
        albumName = song.albumName,
        albumId = song.albumId,
        coverArtId = song.coverArtId
    )

    /**
     * Maps a complete [SearchResult] into view object lists.
     *
     * @return A triple of (artists, albums, songs) view object lists.
     */
    fun map(result: SearchResult): Triple<
        List<SearchArtistViewObject>,
        List<SearchAlbumViewObject>,
        List<SearchSongViewObject>
        > = Triple(
        result.artists.map(::mapArtist),
        result.albums.map(::mapAlbum),
        result.songs.map(::mapSong)
    )
}
