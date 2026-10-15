package com.vsulimov.resonance

import android.app.Application
import com.vsulimov.resonance.core.di.AppContainer
import com.vsulimov.resonance.core.di.DefaultAppContainer

/**
 * Base Application class for Resonance.
 * Holds the global [AppContainer] for manual Dependency Injection.
 */
class ResonanceApplication : Application() {

    /**
     * AppContainer instance used by the rest of classes to obtain dependencies.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        container.downloadRepository.resumeActiveDownloadsOnAppStart()
    }
}
