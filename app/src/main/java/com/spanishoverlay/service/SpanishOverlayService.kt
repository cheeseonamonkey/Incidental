package com.spanishoverlay.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.LearningRepository
import com.spanishoverlay.data.LearningSelection
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
    private var learningRepository: LearningRepository? = null
    private var debouncer: EventDebouncer? = null
    @Volatile private var currentConfig = OverlayConfig.DEFAULT
    @Volatile private var connected = false

    override fun onServiceConnected() {
        if (connected) return
        val cfg = ConfigRepository.getInstance(this)
        val learning = LearningRepository.getInstance(this)
        connected = true
        learningRepository = learning
        overlayManager = OverlayManager(this, cfg)
        walker = NodeWalker()
        pipeline = WordFilterPipeline(cfg, learning)
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
        val learning = learningRepository

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            if (currentConfig.selectedTextActionsEnabled) handleSelectionEvent(event, learning)
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                om.clearAll()
                if (!currentConfig.rescanAfterClearEvents) return
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!isContentChangeRelevant(event.contentChangeTypes)) return
            }
            else -> return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg in currentConfig.excludePackages) return
        if (pkg == packageName) return

        try {
            val focusedId = windows?.find { it.isFocused }?.id ?: -1
            if (focusedId >= 0 && event.windowId != focusedId) return
        } catch (_: Exception) {}

        debouncer?.debounce {
            runCatching { SpanishDictionary.ensureLoaded(this@SpanishOverlayService) }
                .onFailure {
                    Log.w("SpanishOverlay", "dict load: ${it.message}")
                    return@debounce
                }
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

    private fun isContentChangeRelevant(types: Int): Boolean {
        if (types == 0) return true
        val mask = AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE
        return types and mask != 0
    }

    private fun handleSelectionEvent(event: AccessibilityEvent, learning: LearningRepository?) {
        val selected = extractSelectedText(event) ?: return
        runCatching { SpanishDictionary.ensureLoaded(this) }.getOrElse { return }
        val entry = SpanishDictionary.findAny(selected, currentConfig.normalizationEnabled) ?: return
        learning?.setSelection(LearningSelection(entry.key, entry.english, entry.spanish, selected, entry.pos))
    }

    private fun extractSelectedText(event: AccessibilityEvent): String? {
        val full = event.source?.text?.toString()
            ?: event.text.firstOrNull()?.toString()
            ?: return null
        val start = event.fromIndex.coerceAtLeast(0)
        val end = event.toIndex.coerceAtMost(full.length)
        if (start >= end || start >= full.length) return null
        return full.substring(start, end).trim().takeIf { it.isNotBlank() }
    }
}
