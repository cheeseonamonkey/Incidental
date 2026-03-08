package com.spanishoverlay.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class NodeWalker(private val maxDepth: Int = 15) {

    private val SKIP_CLASSES = setOf(
        "android.widget.EditText",
        "android.widget.AutoCompleteTextView",
        "android.inputmethodservice.ExtractEditText",
        "androidx.compose.ui.platform.ComposeView"
    )

    fun walk(root: AccessibilityNodeInfo): List<NodeSnapshot> {
        val results = mutableListOf<NodeSnapshot>()
        val seenBounds = mutableSetOf<String>()
        data class Entry(val node: AccessibilityNodeInfo, val depth: Int)
        val queue = ArrayDeque<Entry>()
        queue.addLast(Entry(root, 0))

        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()
            val cls = node.className?.toString()

            val skip = depth > maxDepth
                || !node.isVisibleToUser
                || cls in SKIP_CLASSES
                || node.isPassword
                || node.isFocused

            if (!skip) {
                val text = node.text?.toString()
                val isLeaf = node.childCount == 0
                // Capture text from leaves always; from non-leaves only if no child has text
                // (avoids duplicating parent text that's really from a child)
                val shouldCapture = !text.isNullOrBlank() && (isLeaf || !hasChildWithText(node))
                if (shouldCapture) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    val boundsKey = bounds.toShortString()
                    if (!bounds.isEmpty && seenBounds.add(boundsKey)) {
                        val key = node.viewIdResourceName?.takeIf { it.isNotEmpty() }
                            ?: "$cls:$boundsKey"
                        results.add(NodeSnapshot(text!!, bounds, key))
                    }
                }
                // Always enqueue children for deeper traversal
                if (!isLeaf && depth < maxDepth) {
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let { queue.addLast(Entry(it, depth + 1)) }
                    }
                }
            }
            if (node !== root) try { node.recycle() } catch (_: Exception) {}
        }
        return results
    }

    /** Quick check if any direct child has non-blank text. */
    private fun hasChildWithText(node: AccessibilityNodeInfo): Boolean {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val has = !child.text.isNullOrBlank()
            try { child.recycle() } catch (_: Exception) {}
            if (has) return true
        }
        return false
    }
}
