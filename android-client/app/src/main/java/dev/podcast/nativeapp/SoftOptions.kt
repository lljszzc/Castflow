package dev.podcast.nativeapp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable fun SoftOptions(title: String, actions: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val colors = CastflowTokens.colors
    BackHandler(onBack = onDismiss)
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { open = true }
    Box(Modifier.fillMaxSize(),contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(colors.scrim).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss))
        AnimatedVisibility(open, enter = slideInVertically(tween(280,easing = MotionTokens.enter)) { it } + fadeIn()) {
            Glass(Modifier.padding(bottom = dockBottomLift()).imePadding().padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                    LocalizedText(title,Modifier.padding(horizontal = 22.dp,vertical = 12.dp),fontSize = 20.sp,fontWeight = FontWeight.Bold,color = colors.text)
                    actions.forEach { (label, action) -> LocalizedText(label,Modifier.fillMaxWidth().glassTap(onClick = action).padding(horizontal = 22.dp,vertical = 14.dp),fontSize = 17.sp,color = colors.text) }
                    LocalizedText("取消",Modifier.fillMaxWidth().glassTap(onClick = onDismiss).padding(horizontal = 22.dp,vertical = 14.dp),color = colors.secondaryText)
                }
            }
        }
    }
}

@Composable fun SoftSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.(closeWith: (() -> Unit) -> Unit) -> Unit
) {
    val colors = CastflowTokens.colors
    val resolvedContainer = if (containerColor == Color.Unspecified) colors.background else containerColor
    val resolvedContent = if (contentColor == Color.Unspecified) colors.text else contentColor
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(280, easing = MotionTokens.enter)) }

    fun closeWith(action: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(260, easing = MotionTokens.exit))
            action()
        }
    }
    val dismiss = { closeWith(onDismissRequest) }
    BackHandler(onBack = dismiss)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }
            .background(colors.scrim)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null, onClick = dismiss))
        val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
        val height = with(androidx.compose.ui.platform.LocalDensity.current) { window.height.toDp() }
        Glass(Modifier.graphicsLayer {
            translationY = size.height * (1f - progress.value)
            alpha = progress.value
        }.padding(bottom = dockBottomLift()).imePadding().padding(horizontal = 16.dp)
            .fillMaxWidth().heightIn(max = height * .88f)) {
            CompositionLocalProvider(LocalContentColor provides resolvedContent) {
                Column(Modifier.fillMaxWidth().background(resolvedContainer.copy(alpha = .45f))) {
                    val dismissLabel = stringResource(R.string.dismiss_sheet)
                    Box(Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = dismissLabel }
                        .glassTap(onClick = dismiss), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(36.dp,4.dp).background(resolvedContent.copy(alpha = .2f)))
                    }
                    content(::closeWith)
                }
            }
        }
        if (closing) {
            // Consume taps while the sheet is still covering the underlying page.
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            }, indication = null) {})
        }
    }
}

@Composable fun SoftDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    onConfirm: () -> Unit = onDismissRequest,
    showCancel: Boolean = false,
    text: @Composable (closeWith: (() -> Unit) -> Unit) -> Unit
) {
    SoftSheet(onDismissRequest) { closeWith ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp,vertical = 10.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
            title()
            Box(Modifier.weight(1f,fill = false)) { text(closeWith) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) {
                if (showCancel) SoftTextButton({ closeWith(onDismissRequest) }) { LocalizedText("取消") }
                SoftTextButton({ closeWith(onConfirm) }) { LocalizedText("完成") }
            }
        }
    }
}
