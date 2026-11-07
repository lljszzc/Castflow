package dev.podcast.nativeapp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch

@Composable fun GlassMenu(actions: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val offsetY = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.roundToPx() }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) { progress.animateTo(1f,tween(220,easing = MotionTokens.enter)) }
    fun close(action: (() -> Unit)? = null) {
        if(closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f,tween(150,easing = MotionTokens.exit))
            dismiss()
            action?.invoke()
        }
    }
    Popup(alignment = Alignment.TopEnd, offset = IntOffset(0,offsetY), onDismissRequest = { close() }, properties = PopupProperties(focusable = true)) {
        Glass(Modifier.width(174.dp).graphicsLayer {
            transformOrigin = TransformOrigin(1f,0f)
            alpha = progress.value
            scaleX = .92f + .08f*progress.value
            scaleY = scaleX
            translationY = (1f-progress.value)*-8.dp.toPx()
        }) { Column(Modifier.padding(6.dp)) {
            actions.forEachIndexed { index, (title, action) ->
                LocalizedText(title, modifier = Modifier.fillMaxWidth()
                    .background(if(selected == index) Color.Black.copy(alpha = .09f) else Color.Transparent,RoundedCornerShape(16.dp))
                    .glassTap { if(!closing) { selected = index; close(action) } }
                    .padding(horizontal = 12.dp, vertical = 15.dp), color = Color.Black, fontSize = 16.sp)
            }
        } }
    }
}
