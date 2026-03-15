package com.vsulimov.resonance.ui.screen.main

import com.vsulimov.resonance.domain.usecase.LoadCredentialsUseCase
import com.vsulimov.resonance.ui.navigation.MainTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the main application shell.
 *
 * Manages the currently selected bottom navigation tab and loads the
 * authenticated user's initial for the avatar displayed in the top bar.
 *
 * This class is not a ViewModel — it is created by [MainShellViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param loadCredentialsUseCase Use case for retrieving stored credentials.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class MainShellStateHolder(
    loadCredentialsUseCase: LoadCredentialsUseCase,
    scope: CoroutineScope
) {

    private val _selectedTab = MutableStateFlow(MainTab.MIX)

    /** Currently selected bottom navigation tab. Defaults to [MainTab.MIX]. */
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _avatarInitial = MutableStateFlow("")

    /**
     * Single uppercase character representing the logged-in user,
     * derived from the first character of the stored username.
     * Falls back to [FALLBACK_INITIAL] if credentials are unavailable.
     */
    val avatarInitial: StateFlow<String> = _avatarInitial.asStateFlow()

    init {
        scope.launch {
            val credentials = loadCredentialsUseCase()
            _avatarInitial.value = credentials?.username
                ?.firstOrNull()
                ?.uppercase()
                ?: FALLBACK_INITIAL
        }
    }

    /**
     * Updates the currently selected tab.
     *
     * @param tab The tab to select.
     */
    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    private companion object {
        const val FALLBACK_INITIAL = "?"
    }
}
