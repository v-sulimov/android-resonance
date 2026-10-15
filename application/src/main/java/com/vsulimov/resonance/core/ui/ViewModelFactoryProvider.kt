package com.vsulimov.resonance.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.vsulimov.resonance.ResonanceApplication
import com.vsulimov.resonance.core.di.AppContainer

/**
 * Extension to grab the [AppContainer] from the Application context.
 */
fun CreationExtras.resonanceApplication(): ResonanceApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ResonanceApplication)

/**
 * Convenience property to access the [AppContainer] directly from [CreationExtras].
 */
val CreationExtras.appContainer: AppContainer
    get() = resonanceApplication().container

/**
 * Extension function to retrieve [AppContainer] from any [Context].
 */
fun Context.getAppContainer(): AppContainer = (applicationContext as ResonanceApplication).container

/**
 * Helper composable to get the [AppContainer] anywhere in the UI tree without
 * needing to pass it down explicitly through parameters.
 */
@Composable
fun getAppContainer(): AppContainer {
    val application = LocalContext.current.applicationContext as ResonanceApplication
    return application.container
}
