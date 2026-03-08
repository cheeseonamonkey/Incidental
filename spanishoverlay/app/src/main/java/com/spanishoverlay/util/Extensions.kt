package com.spanishoverlay.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

fun Context.isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
    return try {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val id = "$packageName/${serviceClass.name}"
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id == id }
    } catch (_: Exception) { false }
}
