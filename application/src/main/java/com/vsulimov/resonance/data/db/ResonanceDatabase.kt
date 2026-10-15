package com.vsulimov.resonance.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vsulimov.resonance.data.db.dao.AlbumDao
import com.vsulimov.resonance.data.db.dao.ArtistDao
import com.vsulimov.resonance.data.db.dao.DownloadTaskDao
import com.vsulimov.resonance.data.db.dao.PlaylistDao
import com.vsulimov.resonance.data.db.dao.SongDao
import com.vsulimov.resonance.data.db.entity.DownloadTaskEntity
import com.vsulimov.resonance.data.db.entity.OfflineAlbumEntity
import com.vsulimov.resonance.data.db.entity.OfflineArtistEntity
import com.vsulimov.resonance.data.db.entity.OfflinePlaylistEntity
import com.vsulimov.resonance.data.db.entity.OfflineSongEntity
import com.vsulimov.resonance.data.db.entity.PlaylistSongCrossRefEntity

/**
 * Main Room database for Resonance offline metadata, playback persistence, and download tasks.
 */
@Database(
    entities = [
        OfflineSongEntity::class,
        OfflineAlbumEntity::class,
        OfflineArtistEntity::class,
        OfflinePlaylistEntity::class,
        PlaylistSongCrossRefEntity::class,
        DownloadTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ResonanceDatabase : RoomDatabase() {

    /**
     * Provides Data Access Object for offline songs.
     *
     * @return [SongDao] instance.
     */
    abstract fun songDao(): SongDao

    /**
     * Provides Data Access Object for offline albums.
     *
     * @return [AlbumDao] instance.
     */
    abstract fun albumDao(): AlbumDao

    /**
     * Provides Data Access Object for offline artists.
     *
     * @return [ArtistDao] instance.
     */
    abstract fun artistDao(): ArtistDao

    /**
     * Provides Data Access Object for offline playlists.
     *
     * @return [PlaylistDao] instance.
     */
    abstract fun playlistDao(): PlaylistDao

    /**
     * Provides Data Access Object for download tasks.
     *
     * @return [DownloadTaskDao] instance.
     */
    abstract fun downloadTaskDao(): DownloadTaskDao
}
