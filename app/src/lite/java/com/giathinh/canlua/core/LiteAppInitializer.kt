package com.giathinh.canlua.core

import android.app.Application
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteAppInitializer @Inject constructor() : AppInitializer {

    override fun init(application: Application) {
        Log.d("CanLuaLite", "Lite edition initialized: 100% offline mode")
    }
}
