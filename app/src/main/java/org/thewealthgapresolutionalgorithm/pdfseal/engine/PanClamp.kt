package org.thewealthgapresolutionalgorithm.pdfseal.engine

/**
 * Pure, Android-free pan math so it is fully JVM unit-testable. Keeps the page
 * from being flung completely off-screen: at least 20% of the smaller of the
 * (scaled page, viewport) stays visible on the axis.
 */
object PanClamp {
    fun clampAxis(pan: Float, scaledSizePx: Float, viewportPx: Float): Float {
        val keep = minOf(scaledSizePx, viewportPx) * 0.2f
        val limit = (scaledSizePx / 2f + viewportPx / 2f - keep)
            .coerceAtLeast(0f)
        return pan.coerceIn(-limit, limit)
    }
}
