package com.spanishoverlay.overlay

import android.content.Context

class OverlayPool(private val context: Context) {
    private val pool = ArrayDeque<OverlayView>(30)
    fun acquire(): OverlayView = pool.removeLastOrNull() ?: OverlayView(context)
    fun release(view: OverlayView) {
        view.reset()
        if (pool.size < 30) pool.addLast(view)
    }
}
