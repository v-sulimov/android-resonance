package com.vsulimov.resonance.di

import android.content.Context
import com.vsulimov.resonance.core.SessionStateHolder
import com.vsulimov.resonance.data.remote.SubsonicClientProvider
import com.vsulimov.resonance.data.repository.AlbumRepositoryImpl
import com.vsulimov.resonance.data.repository.ArtistRepositoryImpl
import com.vsulimov.resonance.data.repository.CoverArtRepositoryImpl
import com.vsulimov.resonance.data.repository.CredentialsRepositoryImpl
import com.vsulimov.resonance.data.repository.LibraryRepositoryImpl
import com.vsulimov.resonance.data.repository.PreferencesRepositoryImpl
import com.vsulimov.resonance.data.repository.SearchRepositoryImpl
import com.vsulimov.resonance.data.repository.ServerRepositoryImpl
import com.vsulimov.resonance.data.repository.SongRepositoryImpl
import com.vsulimov.resonance.domain.repository.AlbumRepository
import com.vsulimov.resonance.domain.repository.ArtistRepository
import com.vsulimov.resonance.domain.repository.CoverArtRepository
import com.vsulimov.resonance.domain.repository.CredentialsRepository
import com.vsulimov.resonance.domain.repository.LibraryRepository
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import com.vsulimov.resonance.domain.repository.SearchRepository
import com.vsulimov.resonance.domain.repository.ServerRepository
import com.vsulimov.resonance.domain.repository.SongRepository
import com.vsulimov.resonance.domain.usecase.ConnectToServerUseCase
import com.vsulimov.resonance.domain.usecase.GetAlbumDetailUseCase
import com.vsulimov.resonance.domain.usecase.GetAlbumListUseCase
import com.vsulimov.resonance.domain.usecase.GetArtistListUseCase
import com.vsulimov.resonance.domain.usecase.GetCoverArtUseCase
import com.vsulimov.resonance.domain.usecase.GetLibraryCountsUseCase
import com.vsulimov.resonance.domain.usecase.GetMixAlbumsUseCase
import com.vsulimov.resonance.domain.usecase.GetSongListUseCase
import com.vsulimov.resonance.domain.usecase.LoadCredentialsUseCase
import com.vsulimov.resonance.domain.usecase.LogoutUseCase
import com.vsulimov.resonance.domain.usecase.RefreshRandomPicksUseCase
import com.vsulimov.resonance.domain.usecase.SearchUseCase

/**
 * Manual dependency injection container for the Resonance application.
 *
 * Creates and wires all repository implementations, use cases, and
 * application-wide state holders. Held as a singleton by
 * [ResonanceApplication][com.vsulimov.resonance.ResonanceApplication]
 * and accessed from ViewModels via the application context.
 *
 * @param context Application context used by repositories that need Android system services.
 */
class AppContainer(context: Context) {

    /** Application-wide session lifecycle events (authentication failures). */
    val sessionStateHolder: SessionStateHolder = SessionStateHolder()

    /** Credential storage backed by [SharedPreferences][android.content.SharedPreferences]. */
    val credentialsRepository: CredentialsRepository = CredentialsRepositoryImpl(context)

    /** User preferences and server metadata storage. */
    val preferencesRepository: PreferencesRepository = PreferencesRepositoryImpl(context)

    /** Server connectivity via [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient]. */
    val serverRepository: ServerRepository = ServerRepositoryImpl()

    /** Lazily-created, cached [SubsonicClient][com.vsulimov.libsubsonic.SubsonicClient] for ongoing API calls. */
    val subsonicClientProvider: SubsonicClientProvider = SubsonicClientProvider(credentialsRepository)

    /** Album list retrieval from the Subsonic server. */
    val albumRepository: AlbumRepository = AlbumRepositoryImpl(subsonicClientProvider)

    /** Artist list retrieval from the Subsonic server. */
    val artistRepository: ArtistRepository = ArtistRepositoryImpl(subsonicClientProvider)

    /** Cover art retrieval with in-memory LRU caching. */
    val coverArtRepository: CoverArtRepository = CoverArtRepositoryImpl(subsonicClientProvider)

    /** Song list retrieval via search API. */
    val songRepository: SongRepository = SongRepositoryImpl(subsonicClientProvider)

    /** Full-text search across artists, albums, and songs. */
    val searchRepository: SearchRepository = SearchRepositoryImpl(subsonicClientProvider)

    /** Library-wide category counts (artists, albums, songs, playlists, genres, favorites). */
    val libraryRepository: LibraryRepository = LibraryRepositoryImpl(subsonicClientProvider)

    /** Pings the server and persists credentials and server info on success. */
    val connectToServerUseCase: ConnectToServerUseCase = ConnectToServerUseCase(
        serverRepository = serverRepository,
        credentialsRepository = credentialsRepository,
        preferencesRepository = preferencesRepository
    )

    /** Loads stored credentials to check authentication state. */
    val loadCredentialsUseCase: LoadCredentialsUseCase = LoadCredentialsUseCase(
        credentialsRepository = credentialsRepository
    )

    /** Clears credentials and preferences, invalidates the client, and signals session expiry. */
    val logoutUseCase: LogoutUseCase = LogoutUseCase(
        credentialsRepository = credentialsRepository,
        preferencesRepository = preferencesRepository,
        sessionStateHolder = sessionStateHolder,
        clientInvalidator = subsonicClientProvider
    )

    /** Fetches full album details with track listing. */
    val getAlbumDetailUseCase: GetAlbumDetailUseCase = GetAlbumDetailUseCase(
        albumRepository = albumRepository
    )

    /** Fetches paginated album lists sorted alphabetically. */
    val getAlbumListUseCase: GetAlbumListUseCase = GetAlbumListUseCase(
        albumRepository = albumRepository
    )

    /** Fetches all artists from the library. */
    val getArtistListUseCase: GetArtistListUseCase = GetArtistListUseCase(
        artistRepository = artistRepository
    )

    /** Fetches all four Mix tab album carousels in parallel. */
    val getMixAlbumsUseCase: GetMixAlbumsUseCase = GetMixAlbumsUseCase(
        albumRepository = albumRepository
    )

    /** Fetches and caches cover art images. */
    val getCoverArtUseCase: GetCoverArtUseCase = GetCoverArtUseCase(
        coverArtRepository = coverArtRepository
    )

    /** Fetches a fresh random album selection for the Mix tab. */
    val refreshRandomPicksUseCase: RefreshRandomPicksUseCase = RefreshRandomPicksUseCase(
        albumRepository = albumRepository
    )

    /** Fetches paginated song lists for the Songs list screen. */
    val getSongListUseCase: GetSongListUseCase = GetSongListUseCase(
        songRepository = songRepository
    )

    /** Searches the library for artists, albums, and songs. */
    val searchUseCase: SearchUseCase = SearchUseCase(
        searchRepository = searchRepository
    )

    /** Fetches all library category counts in parallel for the Library tab. */
    val getLibraryCountsUseCase: GetLibraryCountsUseCase = GetLibraryCountsUseCase(
        libraryRepository = libraryRepository
    )
}
