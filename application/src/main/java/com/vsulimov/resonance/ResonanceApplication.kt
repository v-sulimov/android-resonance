package com.vsulimov.resonance

import android.app.Application
import com.vsulimov.resonance.di.AppContainer

/**
 * Application subclass that hosts the manual dependency injection container.
 *
 * [appContainer] is initialized in [onCreate] and provides the full object
 * graph (repositories, use cases) to ViewModels and other components.
 *
 * Must be declared in `AndroidManifest.xml` via `android:name=".ResonanceApplication"`.
 */
class ResonanceApplication : Application() {

    /** Root dependency injection container for the application. */
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
