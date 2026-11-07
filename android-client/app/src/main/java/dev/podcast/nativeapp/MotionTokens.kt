package dev.podcast.nativeapp
import androidx.compose.animation.core.*

// App tuning, not claimed as official Xiaomi timing constants.
object MotionTokens {
    val enter = CubicBezierEasing(.18f,.78f,.22f,1f)
    val exit = CubicBezierEasing(.35f,0f,.18f,1f)
    fun playerEnter() = tween<Float>(460,easing = enter)
    fun playerExit() = tween<Float>(420,easing = exit)
    fun recover() = tween<Float>(270,easing = FastOutSlowInEasing)
    fun press() = tween<Float>(160,easing = FastOutSlowInEasing)
    const val pressedScale = .97f
}
