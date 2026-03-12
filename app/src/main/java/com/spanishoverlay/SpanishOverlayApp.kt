package com.spanishoverlay

import android.app.Application
import android.util.Log
import com.spanishoverlay.data.SpanishDictionary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpanishOverlayApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        appScope.launch {
            SpanishDictionary.ensureLoaded(applicationContext)
            Log.d("SpanishOverlay", "Dictionary: ${SpanishDictionary.size()} entries")
        }
    }

    companion object {
        lateinit var INSTANCE: SpanishOverlayApp
            private set
    }
}
