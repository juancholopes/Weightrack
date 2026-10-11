package com.juancholopes.weightrack

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

/** Creates the single [AppContainer] of the app (S20-R07). */
class WeightrackApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * The container, read by ViewModel factories inside `viewModelFactory { initializer { ... } }` (ADR-30-04,
 * S30-R16). `rememberViewModelStoreNavEntryDecorator()` passes the activity's default extras, which carry
 * [APPLICATION_KEY].
 */
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as WeightrackApplication).container
