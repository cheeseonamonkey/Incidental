package com.spanishoverlay

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.spanishoverlay.data.SpanishDictionary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpanishOverlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            SpanishDictionary.findAny("warmup")
            Log.d("SpanishOverlay", "Dictionary: ${SpanishDictionary.size()} entries")
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().detectNetwork()
                    .penaltyLog().build()
            )
        }
    }
}
