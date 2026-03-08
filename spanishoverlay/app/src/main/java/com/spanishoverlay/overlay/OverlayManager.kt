package com.spanishoverlay.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import com.spanishoverlay.pipeline.PipelineResult
import com.spanishoverlay.util.StatusBarHeight

@MainThread
class OverlayManager(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val pool = OverlayPool(context)
    private val active = LinkedHashMap<String, ActiveOverlay>(MAX_OVERLAYS + 4)
    private val handler = Handler(Looper.getMainLooper())
    private val statusBarH by lazy { StatusBarHeight.get(context) }
    private val density by lazy { context.resources.displayMetrics.density.coerceAtLeast(1f) }
    private val minW by lazy { (40 * density).toInt().coerceAtLeast(80) }

    data class ActiveOverlay(val view: OverlayView, val bounds: Rect)

    companion object { const val MAX_OVERLAYS = 20 }

    private fun getDisplayWidth(): Int {
        val w = context.resources.displayMetrics.widthPixels
        return if (w > 0) w else 1080 // fallback for edge cases
    }

    fun show(bounds: Rect, key: String, result: PipelineResult) {
        if (bounds.isEmpty || result.replacements.isEmpty()) return

        // Dedup: skip if another overlay's bounds overlap within +-8px
        val fBounds = RectF(bounds); fBounds.inset(-8f, -8f)
        if (active.values.any {
                RectF(it.bounds).intersects(fBounds.left, fBounds.top, fBounds.right, fBounds.bottom)
            }) return

        // LRU evict
        while (active.size >= MAX_OVERLAYS) {
            active.keys.firstOrNull()?.let { removeByKey(it) } ?: break
        }
        removeByKey(key)

        val view = pool.acquire()
        view.bind(result.replacements, result.alpha)
        val dw = getDisplayWidth()
        val maxW = (dw - minW).coerceAtLeast(minW)
        val w = bounds.width().coerceIn(minW, maxW)
        // Clamp x so overlay doesn't go off-screen
        val x = bounds.left.coerceIn(0, (dw - w).coerceAtLeast(0))

        val params = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            x, bounds.top - statusBarH,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).also { it.gravity = Gravity.TOP or Gravity.START }

        runCatching { wm.addView(view, params) }
            .onSuccess {
                active[key] = ActiveOverlay(view, Rect(bounds))
                handler.postDelayed({ removeByKey(key) }, result.ttlMs.toLong().coerceIn(500, 30000))
            }
            .onFailure { e ->
                pool.release(view)
                Log.w("OverlayManager", "addView: ${e.javaClass.simpleName}")
            }
    }

    fun clearAll() {
        handler.removeCallbacksAndMessages(null)
        active.keys.toList().forEach { removeByKey(it) }
    }

    private fun removeByKey(key: String) {
        active.remove(key)?.let { (view, _) ->
            runCatching { wm.removeView(view) }
            pool.release(view)
        }
    }
}
