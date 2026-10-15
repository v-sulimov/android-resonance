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
import com.vsulimov.resonance.core.ui.appContainer
import com.vsulimov.resonance.data.credentials.CredentialsRepository
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State representing the different sections on the Home Screen.
 */
data class HomeUiState(
    val username: String = "",
    val isLoading: Boolean = true,
    val recentlyAdded: List<Album> = emptyList(),
    val recentlyPlayed: List<Album> = emptyList(),
    val mostPlayed: List<Album> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel responsible for managing the Home Screen state.
 */
class HomeViewModel(
    private val credentialsRepository: CredentialsRepository,
    private val getSubsonicClient: () -> SubsonicClient?
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** The UI state of the home screen, including carousels data. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
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
     * Loads the initial data for the Home screen, parallelizing the requests.
     */
    fun loadData() {
        val creds = credentialsRepository.getCredentials()
        if (creds == null) {
            _uiState.update { it.copy(error = null, isLoading = false) }
            return
        }

        _uiState.update { it.copy(username = creds.username, isLoading = true, error = null) }

        viewModelScope.launch {
            val client = getSubsonicClient()
            if (client == null) {
                _uiState.update { it.copy(error = null, isLoading = false) }
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    // Fetch all lists in parallel
                    val recentDef = async { client.getAlbumList(AlbumListType.NEWEST, size = 15) }
                    val playedDef = async { client.getAlbumList(AlbumListType.RECENT, size = 15) }
                    val frequentDef = async { client.getAlbumList(AlbumListType.FREQUENT, size = 15) }
                    val randomDef = async { client.getAlbumList(AlbumListType.RANDOM, size = 15) }

                    val recentRes = recentDef.await()
                    val playedRes = playedDef.await()
                    val frequentRes = frequentDef.await()
                    val randomRes = randomDef.await()

                    // Extract data from results, defaulting to empty list on failure
                    val recent = if (recentRes is SubsonicResult.Success) recentRes.data.albums else emptyList()
                    val played = if (playedRes is SubsonicResult.Success) playedRes.data.albums else emptyList()
                    val frequent = if (frequentRes is SubsonicResult.Success) frequentRes.data.albums else emptyList()
                    val random = if (randomRes is SubsonicResult.Success) randomRes.data.albums else emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recentlyAdded = recent,
                            recentlyPlayed = played,
                            mostPlayed = frequent,
                            randomAlbums = random
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * Triggers the "Shuffle All" action.
     * In Phase 4, this will fetch random songs and start playback.
     */
    fun shuffleAll() {
        // TODO (Phase 4): Fetch random songs and send to ExoPlayer / MediaSession
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = this.appContainer
                HomeViewModel(
                    credentialsRepository = appContainer.credentialsRepository,
                    getSubsonicClient = { appContainer.getAuthenticatedSubsonicClient() }
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
