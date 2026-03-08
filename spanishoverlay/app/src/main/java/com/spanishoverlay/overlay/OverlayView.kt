package com.spanishoverlay.overlay

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.spanishoverlay.R
import com.spanishoverlay.pipeline.Replacement

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val tv: TextView

    init {
        inflate(context, R.layout.view_overlay, this)
        tv = findViewById(R.id.overlay_text)
    }

    fun bind(replacements: List<Replacement>, alpha: Float) {
        tv.text = replacements.joinToString("  ") { it.spanish }
        this.alpha = alpha
    }

    fun reset() { tv.text = ""; alpha = 1f }
}
