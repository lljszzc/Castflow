package dev.podcast.nativeapp
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
@Composable fun PlaybackLoading(size: Dp, color: Color) {
    val description = localizedString("正在加载音频")
    CircularProgressIndicator(modifier = Modifier.size(size).semantics { contentDescription = description }, color = color, strokeWidth = 2.dp)
}
