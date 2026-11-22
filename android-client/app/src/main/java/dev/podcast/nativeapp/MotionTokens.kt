package dev.podcast.nativeapp
import androidx.compose.animation.core.*

// App tuning, not claimed as official Xiaomi timing constants.
object MotionTokens {
    val enter = CubicBezierEasing(.18f,.78f,.22f,1f)
    val exit = CubicBezierEasing(.35f,0f,.18f,1f)
    fun page() = tween<Float>(240, easing = enter)
    fun themeWipe() = tween<Float>(520, easing = enter)
    fun themeWipeFade() = tween<Float>(480, easing = exit)
    fun playerEnter() = tween<Float>(460,easing = enter)
    fun playerExit() = tween<Float>(620, easing = FastOutSlowInEasing)
    fun recover() = tween<Float>(270,easing = FastOutSlowInEasing)
    fun press() = tween<Float>(160,easing = FastOutSlowInEasing)
    const val pressedScale = .97f
}

internal data class NavigationMotionKey(
    val tab: String,
    val section: String,
    val categoryId: String?,
    val detailId: String?
)

/**
 * Search keeps its last results visible while the debounced query is replaced.
 * Every other navigation boundary must not reuse content from the previous page.
 */
internal fun canReusePageContent(loadedPage: String, pageKey: String, searchRoot: String?): Boolean =
    loadedPage == pageKey || (searchRoot != null && loadedPage.startsWith("$searchRoot/"))
