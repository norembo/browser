package com.nutrisnap.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. @HiltAndroidApp bootstraps DI, and we provide a
 * Hilt-aware WorkManager factory so reminder workers can be injected.
 */
@HiltAndroidApp
class NutriSnapApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
