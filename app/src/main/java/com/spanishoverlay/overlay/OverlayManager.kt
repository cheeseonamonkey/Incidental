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
import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.OverlayPosition
import com.spanishoverlay.pipeline.PipelineResult
import kotlin.random.Random

@MainThread
class OverlayManager(private val context: Context, private val config: ConfigRepository) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val pool = OverlayPool(context)
    private val active = LinkedHashMap<String, ActiveOverlay>(64)
    private val handler = Handler(Looper.getMainLooper())
    private val pendingShow = HashMap<String, Runnable>(64)
    private val pendingFade = HashMap<String, Runnable>(64)
    private val density by lazy { context.resources.displayMetrics.density.coerceAtLeast(1f) }
    private val minW by lazy { (40 * density).toInt().coerceAtLeast(80) }
    private val edgePad by lazy { (12 * density).toInt() }
    private val gapPx by lazy { (4 * density).toInt() }

    data class ActiveOverlay(val view: OverlayView, val bounds: Rect)

    private fun dw() = context.resources.displayMetrics.widthPixels.coerceAtLeast(480)
    private fun dh() = context.resources.displayMetrics.heightPixels.coerceAtLeast(800)

    fun show(bounds: Rect, key: String, result: PipelineResult) {
        if (bounds.isEmpty || result.replacements.isEmpty()) return
        val cfg = config.snapshot()

        val fBounds = RectF(bounds); fBounds.inset(-8f, -8f)
        if (!cfg.allowOverlayOverlap && active.values.any {
                RectF(it.bounds).intersects(fBounds.left, fBounds.top, fBounds.right, fBounds.bottom)
            }) return

        while (active.size >= cfg.maxOverlays.coerceIn(1, 200)) {
            active.keys.firstOrNull()?.let { removeByKey(it) } ?: break
        }
        cancelPending(key)
        removeByKey(key)

        val view = pool.acquire()
        view.bind(result.replacements, cfg.overlayAlpha, cfg.displayMode,
            cfg.overlayTextColor, cfg.overlayBgColor, cfg.fontScale)

        // Measure view to get height for positioning
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec((dw() * 0.7f).toInt(), android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val viewW = view.measuredWidth.coerceAtLeast(minW)
        val viewH = view.measuredHeight.coerceAtLeast(40)
        val safeW = (dw() - edgePad * 2).coerceAtLeast(minW)
        val w = viewW.coerceIn(minW, safeW)
        val x = (bounds.centerX() - w / 2).coerceIn(edgePad, (dw() - w - edgePad).coerceAtLeast(edgePad))

        val offsetPx = (cfg.verticalOffsetDp * density).toInt()
        val y = when (cfg.overlayPosition) {
            OverlayPosition.ABOVE  -> bounds.top - viewH - gapPx + offsetPx
            OverlayPosition.INLINE -> bounds.centerY() - viewH / 2 + offsetPx
            OverlayPosition.BELOW  -> bounds.bottom + gapPx + offsetPx
        }.coerceIn(0, dh() - viewH)

        val params = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            x, y,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).also { it.gravity = Gravity.TOP or Gravity.START }

        val delayMs = if (cfg.showDelayMs > 0)
            Random.nextInt(0, cfg.showDelayMs + 1).toLong()
        else 0L

        val showTask = Runnable {
            pendingShow.remove(key)
            view.alpha = 0f
            runCatching { wm.addView(view, params) }
                .onSuccess {
                    active[key] = ActiveOverlay(view, Rect(bounds))
                    // Fade in
                    view.animate().alpha(cfg.overlayAlpha).setDuration(cfg.fadeInMs.toLong().coerceIn(0, 1000)).start()
                    // Schedule removal with fade out
                    scheduleFade(key, result.ttlMs)
                }
                .onFailure { e ->
                    pool.release(view)
                    Log.w("OverlayManager", "addView: ${e.javaClass.simpleName}")
                }
        }
        pendingShow[key] = showTask
        handler.postDelayed(showTask, delayMs)
    }

    fun clearAll() {
        handler.removeCallbacksAndMessages(null)
        pendingShow.clear()
        pendingFade.clear()
        active.keys.toList().forEach { removeByKey(it) }
    }

    private fun scheduleFade(key: String, ttlMs: Int) {
        pendingFade.remove(key)?.let(handler::removeCallbacks)
        val fadeTask = Runnable {
            pendingFade.remove(key)
            removeByKeyFade(key)
        }
        pendingFade[key] = fadeTask
        handler.postDelayed(fadeTask, ttlMs.toLong().coerceIn(500, 30000))
    }

    private fun cancelPending(key: String) {
        pendingShow.remove(key)?.let(handler::removeCallbacks)
        pendingFade.remove(key)?.let(handler::removeCallbacks)
    }

    private fun removeByKeyFade(key: String) {
        val ov = active[key] ?: return
        val cfg = config.snapshot()
        ov.view.animate().alpha(0f).setDuration(cfg.fadeOutMs.toLong().coerceIn(0, 1000))
            .withEndAction { removeByKey(key) }.start()
    }

    private fun removeByKey(key: String) {
        cancelPending(key)
        active.remove(key)?.let { (view, _) ->
            view.animate().cancel()
            runCatching { wm.removeView(view) }
            pool.release(view)
        }
    }
}
