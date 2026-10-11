package com.juancholopes.weightrack

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Test

/** The extension ViewModel factories use to reach the container (ADR-30-04). It never reads a repository. */
class WeightrackApplicationTest {

    // SF00-A09 / SF00-R10
    @Test
    fun appContainer_readsContainerFromApplicationKey() {
        val application = ApplicationProvider.getApplicationContext<WeightrackApplication>()
        val extras = MutableCreationExtras().apply { set(APPLICATION_KEY, application) }

        assertSame(application.container, extras.appContainer)
    }
}
