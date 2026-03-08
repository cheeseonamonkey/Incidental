package com.spanishoverlay.util

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

object StatusBarHeight {
    @Volatile private var cached = -1

    fun get(context: Context): Int {
        if (cached >= 0) return cached
        cached = when {
            Build.VERSION.SDK_INT >= 30 -> api30(context) ?: legacy(context)
            else -> legacy(context)
        }
        return cached
    }

    /** Invalidate on config change (foldable unfold, rotation). */
    fun invalidate() { cached = -1 }

    private fun api30(context: Context): Int? = try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.currentWindowMetrics.windowInsets
            .getInsets(WindowInsets.Type.statusBars()).top.takeIf { it > 0 }
    } catch (_: Exception) { null }

    private fun legacy(context: Context): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }
}
