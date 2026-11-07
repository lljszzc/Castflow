package dev.podcast.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*

@OptIn(ExperimentalHazeApi::class)
@Composable fun MoreMenuButton(onClick: () -> Unit) {
    val description = localizedString("更多")
    Box(Modifier.size(48.dp).semantics { contentDescription = description }.glassTap(onClick),contentAlignment = Alignment.Center) {
        val style = HazeDefaults.style(backgroundColor = Color.White,
            tint = HazeTint(Color(0xFFBDBDBD).copy(alpha = .5f)),blurRadius = 20.dp,noiseFactor = 0f)
        Box(Modifier.size(40.dp)
            .shadow(2.dp,CircleShape,ambientColor = Color.Black.copy(alpha = .04f),spotColor = Color.Black.copy(alpha = .05f))
            .clip(CircleShape).hazeEffect(LocalGlassState.current,style) { inputScale = HazeInputScale.Fixed(.8f) }
            .background(Color(0xFFBDBDBD).copy(alpha = .4f)),contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val radius = 1.8.dp.toPx()
                listOf(4f,10f,16f).forEach { y -> drawCircle(Color(0xFF242424),radius,androidx.compose.ui.geometry.Offset(size.width/2f,y.dp.toPx())) }
            }
        }
    }
}
