package com.spanishoverlay.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

fun Context.isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val target = ComponentName(this, serviceClass).flattenToShortString()
    return try {
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id == target }
    } catch (_: Exception) { false }
}
