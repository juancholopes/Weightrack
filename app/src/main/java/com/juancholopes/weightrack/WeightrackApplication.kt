package com.juancholopes.weightrack

import android.app.Application

/** Creates the single [AppContainer] of the app (S20-R07). */
class WeightrackApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
