package com.spanishoverlay.data

import android.content.Context
import com.spanishoverlay.SpanishOverlayApp
import kotlinx.coroutines.flow.*

class ConfigRepository private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("overlay_config", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(OverlayConfig.fromPrefs(prefs).normalized())
    val configFlow: StateFlow<OverlayConfig> = _flow.asStateFlow()

    val debounceMsFlow: StateFlow<Int> = configFlow
        .map { it.debounceMs }
        .stateIn(SpanishOverlayApp.INSTANCE.appScope, SharingStarted.Eagerly, OverlayConfig.DEFAULT.debounceMs)

    fun snapshot(): OverlayConfig = _flow.value

    fun update(block: OverlayConfig.() -> OverlayConfig) {
        val next = block(_flow.value).normalized()
        _flow.value = next
        next.persist(prefs)
    }

    companion object {
        @Volatile private var INSTANCE: ConfigRepository? = null
        fun getInstance(ctx: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: ConfigRepository(ctx.applicationContext).also { INSTANCE = it }
        }
    }
}
