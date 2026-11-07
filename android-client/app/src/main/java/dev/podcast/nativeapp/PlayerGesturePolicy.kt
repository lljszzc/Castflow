package dev.podcast.nativeapp
/** Distance and fling intent are independent; a tap or an upward fling never dismisses. */
object PlayerGesturePolicy {
    fun dismiss(distancePx: Float, heightPx: Float, velocityPxPerSecond: Float): Boolean {
        if(!distancePx.isFinite() || !heightPx.isFinite() || !velocityPxPerSecond.isFinite() || heightPx <= 0) return false
        return distancePx > heightPx*.18f || (distancePx > heightPx*.045f && velocityPxPerSecond > 1400f)
    }
}
