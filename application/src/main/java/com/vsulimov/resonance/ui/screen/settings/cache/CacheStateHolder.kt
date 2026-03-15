package com.vsulimov.resonance.ui.screen.settings.cache

import com.vsulimov.resonance.domain.model.CacheLimit
import com.vsulimov.resonance.domain.repository.CoverArtRepository
import com.vsulimov.resonance.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the cache management screen.
 *
 * @property cacheSizeBytes Current cache size in bytes.
 * @property cacheSizeFormatted Human-readable cache size string.
 * @property cacheLimit Current cache limit preference.
 * @property cacheLimitFormatted Human-readable cache limit string.
 * @property usageFraction Cache usage as a fraction of the limit (0.0 to 1.0).
 */
data class CacheScreenState(
    val cacheSizeBytes: Long,
    val cacheSizeFormatted: String,
    val cacheLimit: CacheLimit,
    val cacheLimitFormatted: String,
    val usageFraction: Float
)

/**
 * State holder for the cache management screen.
 *
 * Manages cache size display, cache limit preference, and cache clearing.
 *
 * This class is not a ViewModel — it is created by [CacheViewModel]
 * which provides the coroutine scope and lifecycle management.
 *
 * @param preferencesRepository Persists cache limit preference.
 * @param coverArtRepository Provides cache size information and clearing.
 * @param formatSize Formats a byte count as a human-readable string.
 * @param formatLimit Formats a [CacheLimit] as a human-readable string.
 * @param scope Coroutine scope tied to the owning ViewModel's lifecycle.
 */
class CacheStateHolder(
    private val preferencesRepository: PreferencesRepository,
    private val coverArtRepository: CoverArtRepository,
    private val formatSize: (Long) -> String,
    private val formatLimit: (CacheLimit) -> String,
    private val scope: CoroutineScope
) {

    private val _screenState = MutableStateFlow<CacheScreenState?>(null)

    /** Current UI state of the cache screen, or `null` while loading. */
    val screenState: StateFlow<CacheScreenState?> = _screenState.asStateFlow()

    init {
        scope.launch { loadState() }
    }

    /**
     * Updates the cache limit preference.
     *
     * Persists the change and recalculates the usage fraction.
     */
    fun setCacheLimit(limit: CacheLimit) {
        val current = _screenState.value ?: return
        val sizeBytes = coverArtRepository.getCacheSizeBytes()
        _screenState.value = current.copy(
            cacheLimit = limit,
            cacheLimitFormatted = formatLimit(limit),
            usageFraction = calculateUsageFraction(sizeBytes, limit)
        )
        scope.launch { preferencesRepository.setCacheLimit(limit) }
    }

    /**
     * Evicts all entries from the cover art cache and refreshes the display.
     */
    fun clearCache() {
        coverArtRepository.clearCache()
        refreshState()
    }

    private fun refreshState() {
        val current = _screenState.value ?: return
        val sizeBytes = coverArtRepository.getCacheSizeBytes()
        _screenState.value = current.copy(
            cacheSizeBytes = sizeBytes,
            cacheSizeFormatted = formatSize(sizeBytes),
            usageFraction = calculateUsageFraction(sizeBytes, current.cacheLimit)
        )
    }

    private suspend fun loadState() {
        val cacheLimit = preferencesRepository.getCacheLimit()
        val sizeBytes = coverArtRepository.getCacheSizeBytes()

        _screenState.value = CacheScreenState(
            cacheSizeBytes = sizeBytes,
            cacheSizeFormatted = formatSize(sizeBytes),
            cacheLimit = cacheLimit,
            cacheLimitFormatted = formatLimit(cacheLimit),
            usageFraction = calculateUsageFraction(sizeBytes, cacheLimit)
        )
    }

    private companion object {

        fun calculateUsageFraction(sizeBytes: Long, limit: CacheLimit): Float {
            if (limit.bytes <= 0L || limit == CacheLimit.UNLIMITED) return 0f
            return (sizeBytes.toFloat() / limit.bytes.toFloat()).coerceIn(0f, 1f)
        }
    }
}
