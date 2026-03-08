package com.spanishoverlay.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.spanishoverlay.data.DisplayMode
import com.spanishoverlay.pipeline.Replacement

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val primaryTv: TextView
    private val secondaryTv: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        primaryTv = TextView(context).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        secondaryTv = TextView(context).apply {
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
            visibility = GONE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        addView(primaryTv)
        addView(secondaryTv)
        val pd = (6 * resources.displayMetrics.density).toInt()
        val pd2 = (2 * resources.displayMetrics.density).toInt()
        setPadding(pd, pd2, pd, pd2)
    }

    fun bind(replacements: List<Replacement>, alpha: Float, displayMode: DisplayMode,
             textColor: Int, bgColor: Int, fontScale: Float) {
        this.alpha = alpha

        // Background
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8 * resources.displayMetrics.density
            setColor(bgColor)
        }
        background = bg

        val baseSp = 13f * fontScale
        val smallSp = 10f * fontScale
        primaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp)
        secondaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, smallSp)
        primaryTv.setTextColor(textColor)

        when (displayMode) {
            DisplayMode.SPANISH_ONLY -> {
                primaryTv.text = replacements.joinToString("  ") { it.spanish }
                secondaryTv.visibility = GONE
            }
            DisplayMode.ENGLISH_ARROW_SPANISH -> {
                primaryTv.text = replacements.joinToString("  ") { "${it.english} → ${it.spanish}" }
                secondaryTv.visibility = GONE
            }
            DisplayMode.STACKED -> {
                primaryTv.text = replacements.joinToString("  ") { it.spanish }
                secondaryTv.text = replacements.joinToString("  ") { it.english }
                secondaryTv.visibility = VISIBLE
            }
        }
    }

    fun reset() {
        primaryTv.text = ""
        secondaryTv.text = ""
        secondaryTv.visibility = GONE
        alpha = 1f
    }
}
