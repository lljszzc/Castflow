package dev.podcast.nativeapp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme

fun Modifier.glassTap(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) MotionTokens.pressedScale else 1f,MotionTokens.press(),label = "glassPress")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable fun SoftTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Row(modifier.clip(RoundedCornerShape(20.dp)).then(if(enabled) Modifier.glassTap(onClick) else Modifier).alpha(if(enabled) 1f else .4f).sizeIn(minHeight = 44.dp).padding(horizontal = 12.dp,vertical = 8.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.Center) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { content() }
    }
}

@Composable fun SoftIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(modifier.sizeIn(minWidth = 43.dp,minHeight = 43.dp).alpha(if(enabled) 1f else .35f).then(if(enabled) Modifier.glassTap(onClick) else Modifier),contentAlignment = Alignment.Center) { content() }
}

@Composable fun SoftFilledButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit) {
    Row(modifier.then(if(enabled) Modifier.glassTap(onClick) else Modifier).clip(RoundedCornerShape(26.dp))
        .background(if(enabled) colors.containerColor else colors.disabledContainerColor)
        .sizeIn(minHeight = 48.dp).padding(horizontal = 20.dp,vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.Center) {
        CompositionLocalProvider(LocalContentColor provides if(enabled) colors.contentColor else colors.disabledContentColor) { content() }
    }
}
