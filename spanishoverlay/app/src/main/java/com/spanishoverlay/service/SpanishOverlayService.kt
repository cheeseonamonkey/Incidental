package com.spanishoverlay.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.OverlayConfig
import com.spanishoverlay.data.SpanishDictionary
import com.spanishoverlay.overlay.OverlayManager
import com.spanishoverlay.pipeline.WordFilterPipeline
import kotlinx.coroutines.*

class SpanishOverlayService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var overlayManager: OverlayManager? = null
    private var walker: NodeWalker? = null
    private var pipeline: WordFilterPipeline? = null
    private var config: ConfigRepository? = null
    private var debouncer: EventDebouncer? = null
    @Volatile private var currentConfig = OverlayConfig.DEFAULT
    @Volatile private var connected = false

    override fun onServiceConnected() {
        if (connected) return // guard double-connect on some OEMs
        connected = true
        val cfg = ConfigRepository.getInstance(this)
        config = cfg
        overlayManager = OverlayManager(this)
        walker = NodeWalker()
        pipeline = WordFilterPipeline(SpanishDictionary, cfg)
        debouncer = EventDebouncer(cfg.debounceMsFlow, scope)
        scope.launch { cfg.configFlow.collect { currentConfig = it } }
        Log.d("SpanishOverlay", "Service connected, dict=${SpanishDictionary.size()}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!connected) return
        event ?: return
        val om = overlayManager ?: return
        val w = walker ?: return
        val p = pipeline ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> { om.clearAll(); return }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> Unit
            else -> return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg in currentConfig.excludePackages) return
        // Skip our own package
        if (pkg == packageName) return

        // Split-screen guard
        try {
            val focusedId = windows?.find { it.isFocused }?.id ?: -1
            if (focusedId >= 0 && event.windowId != focusedId) return
        } catch (_: Exception) {}

        debouncer?.debounce {
            val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return@debounce
            try {
                w.walk(root).forEach { snap ->
                    val result = p.process(snap.text)
                    if (result.replacements.isNotEmpty()) {
                        withContext(Dispatchers.Main) { om.show(snap.bounds, snap.key, result) }
                    }
                }
            } catch (e: Exception) {
                Log.w("SpanishOverlay", "walk: ${e.message}")
            } finally {
                try { root.recycle() } catch (_: Exception) {}
            }
        }
    }

    override fun onInterrupt() { runCatching { overlayManager?.clearAll() } }

    override fun onDestroy() {
        connected = false
        runCatching { overlayManager?.clearAll() }
        scope.cancel()
        super.onDestroy()
    }
}
