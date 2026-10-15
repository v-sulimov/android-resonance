package com.vsulimov.resonance.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.libsubsonic.SubsonicClient
import com.vsulimov.libsubsonic.data.response.browsing.Album
import com.vsulimov.libsubsonic.data.response.lists.AlbumListType
import com.vsulimov.libsubsonic.data.result.SubsonicResult
import com.vsulimov.resonance.core.Constants
import com.vsulimov.resonance.core.ui.UiText
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.core.utils.ErrorMapper
import com.vsulimov.resonance.data.credentials.CredentialsRepository
import com.vsulimov.resonance.data.db.DownloadStatus
import com.vsulimov.resonance.data.settings.SettingsRepository
import com.vsulimov.resonance.domain.repository.DownloadRepository
import com.vsulimov.resonance.domain.repository.OfflineRepository
import com.vsulimov.resonance.player.PlayerController
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_PULL_TO_REFRESH_DURATION_MS = 500L

/**
 * State representing the different sections on the Home Screen.
 */
data class HomeUiState(
    val username: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOfflineMode: Boolean = false,
    val recentlyAdded: List<Album> = emptyList(),
    val recentlyPlayed: List<Album> = emptyList(),
    val mostPlayed: List<Album> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val starredAlbums: List<Album> = emptyList(),
    val hasActiveDownloads: Boolean = false,
    val isDownloadQueuePaused: Boolean = false,
    val error: UiText? = null
)

/**
 * ViewModel responsible for managing the Home Screen state.
 */
class HomeViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val settingsRepository: SettingsRepository,
    private val offlineRepository: OfflineRepository,
    private val downloadRepository: DownloadRepository,
    private val getSubsonicClient: () -> SubsonicClient?,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** The UI state of the home screen, including carousels data. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDownloadTasks()
        loadData()
    }

    /**
     * Calculates the appropriate greeting part (morning, afternoon, etc.) based on the device time.
     */
    fun getGreetingTimeOfDay(): TimeOfDay {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..22 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * Loads or refreshes the data for the Home screen, parallelizing the requests.
     *
     * @param isPullToRefresh True if triggered by user pull gesture.
     */
    fun loadData(isPullToRefresh: Boolean = false) {
        val creds = credentialsRepository.getCredentials()
        if (creds == null) {
            _uiState.update { it.copy(error = null, isLoading = false, isRefreshing = false) }
            return
        }

        val startTime = System.currentTimeMillis()

        if (isPullToRefresh) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
        } else {
            _uiState.update { it.copy(username = creds.username, isLoading = true, error = null) }
        }

        if (settingsRepository.isOfflineModeEnabledState.value) {
            viewModelScope.launch {
                val albums = offlineRepository.observeAllAlbums().first()
                val starred = offlineRepository.observeStarredAlbums().first()
                _uiState.update {
                    it.copy(
                        username = creds.username,
                        isLoading = false,
                        isRefreshing = false,
                        isOfflineMode = true,
                        recentlyAdded = albums.take(Constants.HOME_CAROUSEL_ITEM_COUNT),
                        recentlyPlayed = albums.take(Constants.HOME_CAROUSEL_ITEM_COUNT),
                        mostPlayed = albums.take(Constants.HOME_CAROUSEL_ITEM_COUNT),
                        randomAlbums = albums.shuffled().take(Constants.HOME_CAROUSEL_ITEM_COUNT),
                        starredAlbums = starred,
                        error = null
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            val client = getSubsonicClient()
            if (client == null) {
                _uiState.update { it.copy(error = null, isLoading = false, isRefreshing = false) }
                return@launch
            }

            viewModelScope.launch {
                offlineRepository.syncPlaylistsGraph(getSubsonicClient)
            }

            try {
                withContext(Dispatchers.IO) {
                    val recentDef =
                        async { client.getAlbumList(AlbumListType.NEWEST, size = Constants.HOME_CAROUSEL_ITEM_COUNT) }
                    val playedDef =
                        async { client.getAlbumList(AlbumListType.RECENT, size = Constants.HOME_CAROUSEL_ITEM_COUNT) }
                    val frequentDef =
                        async { client.getAlbumList(AlbumListType.FREQUENT, size = Constants.HOME_CAROUSEL_ITEM_COUNT) }
                    val randomDef =
                        async { client.getAlbumList(AlbumListType.RANDOM, size = Constants.HOME_CAROUSEL_ITEM_COUNT) }
                    val starredDef =
                        async { client.getAlbumList(AlbumListType.STARRED, size = Constants.HOME_CAROUSEL_ITEM_COUNT) }

                    val recentRes = recentDef.await()
                    val playedRes = playedDef.await()
                    val frequentRes = frequentDef.await()
                    val randomRes = randomDef.await()
                    val starredRes = starredDef.await()

                    val recent = if (recentRes is SubsonicResult.Success) recentRes.data.albums else emptyList()
                    val played = if (playedRes is SubsonicResult.Success) playedRes.data.albums else emptyList()
                    val frequent = if (frequentRes is SubsonicResult.Success) frequentRes.data.albums else emptyList()
                    val random = if (randomRes is SubsonicResult.Success) randomRes.data.albums else emptyList()
                    val starred = if (starredRes is SubsonicResult.Success) starredRes.data.albums else emptyList()

                    val allFailed = recentRes is SubsonicResult.Failure &&
                        playedRes is SubsonicResult.Failure &&
                        frequentRes is SubsonicResult.Failure &&
                        randomRes is SubsonicResult.Failure &&
                        starredRes is SubsonicResult.Failure

                    val errorMessage = if (allFailed) {
                        ErrorMapper.formatError(recentRes.error)
                    } else {
                        null
                    }

                    if (isPullToRefresh) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime < MIN_PULL_TO_REFRESH_DURATION_MS) {
                            delay(MIN_PULL_TO_REFRESH_DURATION_MS - elapsedTime)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            username = creds.username,
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineMode = false,
                            recentlyAdded = recent,
                            recentlyPlayed = played,
                            mostPlayed = frequent,
                            randomAlbums = random,
                            starredAlbums = starred,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (isPullToRefresh) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < MIN_PULL_TO_REFRESH_DURATION_MS) {
                        delay(MIN_PULL_TO_REFRESH_DURATION_MS - elapsedTime)
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = ErrorMapper.formatError(e)
                    )
                }
            }
        }
    }

    /**
     * Triggers the "Shuffle All" action by fetching random songs from server and queueing them.
     */
    fun shuffleAll() {
        viewModelScope.launch {
            if (settingsRepository.isOfflineModeEnabledState.value) {
                val songs = offlineRepository.observeStarredSongs().first()
                if (songs.isNotEmpty()) {
                    playerController.playQueue(songs.shuffled(), startIndex = 0)
                }
                return@launch
            }
            val client = getSubsonicClient() ?: return@launch
            try {
                val result = withContext(Dispatchers.IO) {
                    client.getRandomSongs(size = 50)
                }
                if (result is SubsonicResult.Success) {
                    playerController.playQueue(result.data.songs, startIndex = 0)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    private fun observeDownloadTasks() {
        viewModelScope.launch {
            combine(
                downloadRepository.observeAllTasks(),
                settingsRepository.isDownloadQueuePausedState
            ) { tasks, isPaused ->
                val hasActive = tasks.any {
                    it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
                }
                Pair(hasActive, isPaused)
            }.collect { (hasActive, isPaused) ->
                _uiState.update {
                    it.copy(
                        hasActiveDownloads = hasActive,
                        isDownloadQueuePaused = isPaused
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                HomeViewModel(
                    credentialsRepository = appContainer.credentialsRepository,
                    settingsRepository = appContainer.settingsRepository,
                    offlineRepository = appContainer.offlineRepository,
                    downloadRepository = appContainer.downloadRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() },
                    playerController = appContainer.playerController
                )
            }
        }
    }
}

/** Enum representing time of day for greeting text selection. */
enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}
