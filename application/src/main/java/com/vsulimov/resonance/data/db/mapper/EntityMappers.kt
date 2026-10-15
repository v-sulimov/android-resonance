package com.vsulimov.resonance.data.db.mapper

import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.browsing.Artist
import com.vsulimov.libsubsonic.data.response.browsing.Child
import com.vsulimov.libsubsonic.data.response.playlists.Playlist
import com.vsulimov.resonance.data.db.entity.OfflineAlbumEntity
import com.vsulimov.resonance.data.db.entity.OfflineArtistEntity
import com.vsulimov.resonance.data.db.entity.OfflinePlaylistEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity

// Extension functions to map between libsubsonic API response models and Room entities.

/**
 * Converts a libsubsonic [Child] song model into an [OfflineSongEntity].
 *
 * @param localFilePath Absolute local file path if downloaded.
 * @param downloadedAt Timestamp of completion.
 * @return Mapped [OfflineSongEntity].
 */
fun Child.toOfflineEntity(
    localFilePath: String? = null,
    downloadedAt: Long = System.currentTimeMillis()
): OfflineSongEntity = OfflineSongEntity(
    id = id,
    title = title,
    album = album,
    albumId = albumId,
    artist = artist,
    artistId = artistId,
    track = track,
    discNumber = null,
    year = year,
    genre = genre,
    coverArt = coverArt,
    size = size,
    contentType = contentType,
    suffix = suffix,
    duration = duration ?: 0,
    bitRate = bitRate,
    path = path,
    localFilePath = localFilePath,
    isStarred = !starred.isNullOrBlank(),
    downloadedAt = downloadedAt
)

/**
 * Converts an [OfflineSongEntity] back into a libsubsonic [Child] model.
 *
 * @return Mapped [Child].
 */
fun OfflineSongEntity.toChild(): Child = Child(
    id = id,
    parent = null,
    isDir = false,
    title = title,
    album = album,
    albumId = albumId,
    artist = artist,
    artistId = artistId,
    track = track,
    year = year,
    genre = genre,
    coverArt = coverArt,
    size = size,
    contentType = contentType,
    suffix = suffix,
    duration = duration,
    bitRate = bitRate,
    path = path,
    starred = if (isStarred) "starred" else null,
    type = "music"
)

/**
 * Converts a libsubsonic [Album] model into an [OfflineAlbumEntity].
 *
 * @return Mapped [OfflineAlbumEntity].
 */
fun Album.toOfflineEntity(): OfflineAlbumEntity = OfflineAlbumEntity(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArt = coverArt,
    songCount = songCount ?: songs.size,
    duration = duration ?: songs.sumOf { it.duration ?: 0 },
    year = year,
    genre = genre,
    created = created,
    isStarred = !starred.isNullOrBlank()
)

/**
 * Converts an [OfflineAlbumEntity] back into a libsubsonic [Album] model.
 *
 * @param songs Optional list of child songs belonging to album.
 * @return Mapped [Album].
 */
fun OfflineAlbumEntity.toAlbum(songs: List<Child> = emptyList()): Album = Album(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArt = coverArt,
    songCount = songCount,
    duration = duration,
    created = created,
    starred = if (isStarred) "starred" else null,
    year = year,
    genre = genre,
    songs = songs
)

/**
 * Converts a libsubsonic [Artist] model into an [OfflineArtistEntity].
 *
 * @return Mapped [OfflineArtistEntity].
 */
fun Artist.toOfflineEntity(): OfflineArtistEntity = OfflineArtistEntity(
    id = id,
    name = name,
    coverArt = coverArt,
    albumCount = albumCount,
    isStarred = !starred.isNullOrBlank()
)

/**
 * Converts an [OfflineArtistEntity] back into a libsubsonic [Artist] model.
 *
 * @param albums Optional list of albums belonging to artist.
 * @return Mapped [Artist].
 */
fun OfflineArtistEntity.toArtist(albums: List<Album> = emptyList()): Artist = Artist(
    id = id,
    name = name,
    coverArt = coverArt,
    albumCount = albumCount,
    starred = if (isStarred) "starred" else null,
    albums = albums
)

/**
 * Converts a libsubsonic [Playlist] model into an [OfflinePlaylistEntity].
 *
 * @return Mapped [OfflinePlaylistEntity].
 */
fun Playlist.toOfflineEntity(): OfflinePlaylistEntity = OfflinePlaylistEntity(
    id = id,
    name = name,
    comment = comment,
    owner = owner,
    public = public,
    songCount = songCount,
    duration = duration,
    created = created,
    changed = null,
    coverArt = coverArt
)

/**
 * Converts an [OfflinePlaylistEntity] back into a libsubsonic [Playlist] model.
 *
 * @param entries Optional list of child songs contained in playlist.
 * @return Mapped [Playlist].
 */
fun OfflinePlaylistEntity.toPlaylist(entries: List<Child> = emptyList()): Playlist = Playlist(
    id = id,
    name = name,
    comment = comment,
    owner = owner,
    public = public ?: false,
    songCount = songCount,
    duration = duration,
    created = created,
    coverArt = coverArt,
    allowedUsers = emptyList(),
    entries = entries
)
