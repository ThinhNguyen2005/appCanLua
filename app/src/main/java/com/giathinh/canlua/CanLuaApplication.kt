package com.giathinh.canlua

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.giathinh.canlua.core.AppInitializer
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CanLuaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: Lazy<HiltWorkerFactory>

    @Inject
    lateinit var appInitializer: AppInitializer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory.get())
            .build()

    override fun onCreate() {
        super.onCreate()
        appInitializer.init(this)
    }
}