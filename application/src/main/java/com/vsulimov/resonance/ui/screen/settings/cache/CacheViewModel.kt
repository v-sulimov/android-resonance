package com.vsulimov.resonance.ui.screen.settings.cache

import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsulimov.resonance.R
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.domain.model.CacheLimit

/**
 * ViewModel for the cache management screen.
 *
 * Scopes the [CacheStateHolder] to the ViewModel lifecycle so that
 * cache data survives configuration changes.
 *
 * @param application Application reference for formatting utilities.
 */
class CacheViewModel(
    application: ResonanceApplication
) : ViewModel() {

    /** State holder managing cache data and user actions. */
    val stateHolder = CacheStateHolder(
        preferencesRepository = application.appContainer.preferencesRepository,
        coverArtRepository = application.appContainer.coverArtRepository,
        formatSize = { bytes -> Formatter.formatShortFileSize(application, bytes) },
        formatLimit = { limit -> formatCacheLimit(application, limit) },
        scope = viewModelScope
    )

    companion object {

        /** Factory that resolves dependencies from [ResonanceApplication.appContainer]. */
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ResonanceApplication
                CacheViewModel(application = application)
            }
        }

        private fun formatCacheLimit(application: ResonanceApplication, limit: CacheLimit): String =
            when (limit) {
                CacheLimit.MB_256 -> application.getString(R.string.cache_limit_256mb)
                CacheLimit.MB_512 -> application.getString(R.string.cache_limit_512mb)
                CacheLimit.GB_1 -> application.getString(R.string.cache_limit_1gb)
                CacheLimit.GB_2 -> application.getString(R.string.cache_limit_2gb)
                CacheLimit.GB_5 -> application.getString(R.string.cache_limit_5gb)
                CacheLimit.UNLIMITED -> application.getString(R.string.cache_limit_unlimited)
            }
    }
}
