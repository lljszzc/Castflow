package dev.podcast.nativeapp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable fun SoftOptions(title: String, actions: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { open = true }
    Box(Modifier.fillMaxSize(),contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .16f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss))
        AnimatedVisibility(open, enter = slideInVertically(tween(280,easing = MotionTokens.enter)) { it } + fadeIn()) {
            Glass(Modifier.padding(bottom = dockBottomLift()).imePadding().padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    LocalizedText(title,Modifier.padding(horizontal = 22.dp,vertical = 12.dp),fontSize = 20.sp,fontWeight = FontWeight.Bold,color = Color.Black)
                    actions.forEach { (label, action) -> LocalizedText(label,Modifier.fillMaxWidth().glassTap(onClick = action).padding(horizontal = 22.dp,vertical = 14.dp),fontSize = 17.sp,color = Color.Black) }
                    LocalizedText("取消",Modifier.fillMaxWidth().glassTap(onClick = onDismiss).padding(horizontal = 22.dp,vertical = 14.dp),color = Color.Gray)
                }
            }
        }
    }
}

@Composable fun SoftSheet(onDismissRequest: () -> Unit, containerColor: Color = Color.White, contentColor: Color = Color.Black, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismissRequest)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxSize(),contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .14f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismissRequest))
        AnimatedVisibility(shown,enter = slideInVertically(tween(280,easing = MotionTokens.enter)) { it } + fadeIn()) {
            val height = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            Glass(Modifier.fillMaxWidth().padding(bottom = dockBottomLift()).imePadding().padding(horizontal = 16.dp).heightIn(max = (height*.88f).dp)) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(Modifier.fillMaxWidth().background(containerColor.copy(alpha = .45f))) {
                        Box(Modifier.fillMaxWidth().height(24.dp).glassTap(onClick = onDismissRequest),contentAlignment = Alignment.Center) { Box(Modifier.size(36.dp,4.dp).background(contentColor.copy(alpha = .2f))) }
                        content()
                    }
                }
            }
        }
    }
}

@Composable fun SoftDialog(onDismissRequest: () -> Unit, title: @Composable () -> Unit, text: @Composable () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit = {}) {
    SoftSheet(onDismissRequest) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp,vertical = 10.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
            title()
            Box(Modifier.weight(1f,fill = false)) { text() }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) { dismissButton(); confirmButton() }
        }
    }
}
