package dev.podcast.nativeapp
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.*
import kotlin.math.*

object DisplaySafety {
    /** Circular screen corner clearance at the floating dock's inset. Units may be px or dp. */
    fun lift(radius: Float, horizontal: Float, navigation: Float, minimum: Float, margin: Float): Float {
        val r = radius.coerceAtLeast(0f)
        val x = horizontal.coerceIn(0f,r)
        val corner = if(r == 0f) 0f else r - sqrt((r*r-(r-x)*(r-x)).coerceAtLeast(0f))
        return max(minimum,max(navigation+margin,corner+margin))
    }
}
@Composable fun dockBottomLift(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val navigation = WindowInsets.navigationBars.getBottom(density)
    val safeBottom = WindowInsets.safeDrawing.getBottom(density)
    val native = view.rootWindowInsets
    val radius = max(native?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,native?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0)
    return with(density) { DisplaySafety.lift(radius.toFloat(),16.dp.toPx(),max(navigation,safeBottom).toFloat(),16.dp.toPx(),6.dp.toPx()).toDp() }
}
