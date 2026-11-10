package dev.podcast.nativeapp

import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private fun clock(ms: Long): String { val total = ms.coerceAtLeast(0) / 1000; return "${total / 60}:${(total % 60).toString().padStart(2,'0')}" }
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FullPlayer(item: PodcastItem, playing: Boolean, loading: Boolean, player: Player?,
    onDismiss: () -> Unit, onToggle: () -> Unit, onSeek: (Long) -> Unit, onSpeed: (Float) -> Unit,
    sourceBounds: androidx.compose.ui.geometry.Rect?, onQueue: () -> Unit, onSave: () -> Unit, onSleep: (Int) -> Unit, onStatusCoverage: (Boolean) -> Unit) {
    val extracted = rememberArtworkColors(item.artwork)
    val palette = extracted.copy(background = extracted.playerBackground, foreground = Color.White)
    val context = LocalContext.current
    val background by animateColorAsState(palette.background.copy(alpha = 1f), label = "playerBackdrop")
    var menu by remember { mutableStateOf(false) }
    var sleep by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    if (window.width <= 0 || window.height <= 0) return
    val heightPx = window.height.toFloat()
    val screenWidth = window.width.toFloat()
    val heightDp = with(density) { window.height.toDp() }
    val widthDp = with(density) { window.width.toDp() }
    val shortLayout = heightDp < 680.dp || density.fontScale > 1.15f
    val coverDp = minOf((widthDp - 60.dp).coerceAtLeast(0.dp) * .88f,
        heightDp * if (shortLayout) .23f else .43f)
    val coverSide = with(density) { coverDp.toPx() }
    val nativeInsets = androidx.compose.ui.platform.LocalView.current.rootWindowInsets
    val fallbackRadius = with(density) { 32.dp.toPx() }
    val cornerRadii = remember(nativeInsets) { (0..3).map { nativeInsets?.getRoundedCorner(it)?.radius?.toFloat() ?: fallbackRadius } }
    val source = remember(window) { sourceBounds ?: androidx.compose.ui.geometry.Rect(0f,heightPx- with(density) { 70.dp.toPx() },screenWidth,heightPx) }
    val offset = remember { Animatable(heightPx) }
    val velocity = remember { VelocityTracker() }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var closing by remember { mutableStateOf(false) }
    var trackingBack by remember { mutableStateOf(false) }
    val closeLabel = stringResource(R.string.collapse_player)
    val statusHeight = WindowInsets.statusBars.getTop(density).toFloat()
    val coverageCallback by rememberUpdatedState(onStatusCoverage)
    LaunchedEffect(statusHeight, source, heightPx) { snapshotFlow { source.top * ((offset.value+dragOffset)/heightPx).coerceIn(0f,1f) <= statusHeight }.collect { coverageCallback(it) } }
    DisposableEffect(Unit) { onDispose { onStatusCoverage(false) } }
    LaunchedEffect(Unit) { offset.snapTo(heightPx); offset.animateTo(0f, MotionTokens.playerEnter()) }
    fun recoverPlayer() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            offset.snapTo((offset.value + dragOffset).coerceIn(0f, heightPx))
            dragOffset = 0f
            offset.animateTo(0f, MotionTokens.recover())
        }
    }
    fun closePlayer() {
        if (closing || trackingBack) return
        closing = true
        scope.launch {
            offset.snapTo((offset.value + dragOffset).coerceIn(0f, heightPx))
            dragOffset = 0f
            offset.animateTo(heightPx, MotionTokens.playerExit())
            onDismiss()
        }
    }
    PredictiveBackHandler(enabled = !menu && !speed && !sleep) { events ->
        if (closing) {
            events.collect {}
            return@PredictiveBackHandler
        }
        trackingBack = true
        try {
            val start = (offset.value + dragOffset).coerceIn(0f, heightPx)
            offset.snapTo(start)
            dragOffset = 0f
            events.collect { event ->
                offset.snapTo(PlayerGesturePolicy.backOffset(start, heightPx, event.progress))
            }
        } catch (cancel: CancellationException) {
            recoverPlayer()
            throw cancel
        } finally {
            trackingBack = false
        }
        closePlayer()
    }
    val collapsed = ((offset.value + dragOffset) / heightPx).coerceIn(0f,1f)
    val expanded = 1f - collapsed
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().graphicsLayer {
        alpha = (expanded / .18f).coerceIn(0f,1f)
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f,0f)
        translationX = source.left * collapsed
        translationY = source.top * collapsed
        val width = size.width + (source.width-size.width)*collapsed
        val height = size.height + (source.height-size.height)*collapsed
        val scale = (width/size.width).coerceAtLeast(.01f)
        scaleX = scale; scaleY = scale
        shape = androidx.compose.foundation.shape.GenericShape { _, _ ->
            fun radius(index: Int): androidx.compose.ui.geometry.CornerRadius {
                val device = cornerRadii[index]
                val r = (device + (source.height/2f-device)*collapsed)/scale
                return androidx.compose.ui.geometry.CornerRadius(r.coerceAtMost(height/scale/2f))
            }
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f,0f,size.width,height/scale,radius(0),radius(1),radius(2),radius(3)))
        }
        clip = true
    }) {
        Surface(Modifier.fillMaxSize().pointerInput(heightPx) {
            detectVerticalDragGestures(onDragStart = { if (!closing && !trackingBack) { velocity.resetTracking(); scope.launch { offset.stop() } } }, onVerticalDrag = { change, amount ->
                if(!closing && !trackingBack && (dragOffset > 0 || amount > 0)) { change.consume(); dragOffset = (dragOffset + amount).coerceAtLeast(0f); velocity.addPosition(change.uptimeMillis,Offset(0f,dragOffset)) }
            }, onDragEnd = {
                if (!closing && !trackingBack) {
                    if (PlayerGesturePolicy.dismiss(dragOffset,heightPx,velocity.calculateVelocity().y)) closePlayer() else recoverPlayer()
                }
            }, onDragCancel = { if (!closing && !trackingBack) recoverPlayer() })
        }, color = androidx.compose.ui.graphics.lerp(Color(0xFFF2F2F5),background,expanded), contentColor = palette.foreground) {
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = expanded*expanded }.systemBarsPadding().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = closeLabel }.glassTap { closePlayer() }, contentAlignment = Alignment.Center) { Box(Modifier.size(48.dp,4.dp).clip(RoundedCornerShape(4.dp)).background(palette.foreground.copy(alpha = .4f))) }
                Spacer(Modifier.height(22.dp))
                Spacer(Modifier.size(coverDp))
                Column(Modifier.fillMaxWidth().weight(1f).then(
                    if (shortLayout) Modifier.verticalScroll(rememberScrollState()) else Modifier
                ), verticalArrangement = if (shortLayout) Arrangement.Top else Arrangement.Bottom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { androidx.compose.material3.Text(item.title, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis); androidx.compose.material3.Text(item.author, fontSize = 20.sp, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        SoftIconButton(onSave, Modifier.background(palette.foreground.copy(alpha = .12f), RoundedCornerShape(50))) { LocalizedIcon(Icons.Rounded.StarOutline, "保存单集") }
                        Box { SoftIconButton(onClick = { menu = true }) { LocalizedIcon(Icons.Rounded.MoreVert, "更多播放操作") }
                            if(menu) GlassMenu(listOf("播放速度" to { speed = true }, "睡眠定时" to { sleep = true }), { menu = false })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    key(item.id) { PlaybackTimeline(player, item.id, palette, onSeek) }
                    Row(Modifier.fillMaxWidth().padding(vertical = if (shortLayout) 10.dp else 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        SoftIconButton(onClick = { player?.let { onSeek((it.currentPosition - 15000).coerceAtLeast(0)) } }, Modifier.size(64.dp), enabled = player?.currentMediaItem?.mediaId == item.id && player.isCurrentMediaItemSeekable && player.duration > 0) { LocalizedIcon(Icons.Rounded.Replay, "后退15秒", Modifier.size(40.dp)) }
                        SoftIconButton(onToggle, Modifier.size(84.dp), enabled = !loading) { if(loading) PlaybackLoading(48.dp,palette.foreground) else LocalizedIcon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if(playing) "暂停" else "播放", Modifier.size(76.dp)) }
                        SoftIconButton(onClick = { player?.let { onSeek((it.currentPosition + 30000).coerceAtMost(it.duration)) } }, Modifier.size(64.dp), enabled = player?.currentMediaItem?.mediaId == item.id && player.isCurrentMediaItemSeekable && player.duration > 0) { LocalizedIcon(Icons.Rounded.Forward30, "前进30秒", Modifier.size(40.dp)) }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        SoftTextButton(onClick = { speed = true }) { LocalizedText("倍速", color = palette.foreground) }
                        SoftIconButton(onClick = { sleep = true }) { LocalizedIcon(Icons.Rounded.Bedtime, "睡眠定时") }
                        SoftIconButton(onQueue) { LocalizedIcon(Icons.AutoMirrored.Rounded.QueueMusic, "待播列表") }
                    }
                }
            }
        }
    }
    // The artwork stays visible and travels to the real mini-player cover instead of fading away.
    val miniSide = with(density) { 29.dp.toPx() }
    val coverX = (screenWidth-coverSide)/2f
    val coverY = statusHeight + with(density) { 70.dp.toPx() }
    val miniX = source.left + with(density) { 11.dp.toPx() }
    val miniY = source.top + (source.height-miniSide)/2f
    val side = coverSide + (miniSide-coverSide)*collapsed
    AsyncImage(remember(item.artwork) { coil.request.ImageRequest.Builder(context).data(item.artwork).size(900).crossfade(false).build() },item.title,Modifier
        .offset { androidx.compose.ui.unit.IntOffset((coverX+(miniX-coverX)*collapsed).toInt(),(coverY+(miniY-coverY)*collapsed).toInt()) }
        .size(with(density) { side.toDp() })
        .clip(RoundedCornerShape((15f-8f*collapsed).dp)))
    }
    if(speed) SoftOptions("播放速度", listOf(.75f,1f,1.25f,1.5f,1.75f,2f).map { rate -> "${rate}×" to { onSpeed(rate); speed = false } }, { speed = false })
    if(sleep) SoftOptions("睡眠定时", listOf(0,15,30,45,60).map { minutes -> (if(minutes == 0) "取消定时" else "$minutes 分钟后暂停") to { onSleep(minutes); sleep = false } }, { sleep = false })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackTimeline(player: Player?, episodeId: String, palette: ArtworkColors, onSeek: (Long) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var duration by remember { mutableLongStateOf(0) }
    var position by remember { mutableLongStateOf(0) }
    var seekable by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    DisposableEffect(player, episodeId) {
        fun update() {
            val active = player?.takeIf { it.currentMediaItem?.mediaId == episodeId }
            duration = active?.duration?.coerceAtLeast(0) ?: 0
            position = active?.currentPosition?.coerceAtLeast(0) ?: 0
            seekable = duration > 0 && active?.isCurrentMediaItemSeekable == true
            moving = active?.isPlaying == true
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = update()
        }
        update()
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player, moving, lifecycle) {
        if (moving && player != null) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                position = player.currentPosition.coerceAtLeast(0)
                delay(500)
            }
        }
    }
    var preview by remember(seekable) { mutableStateOf<Float?>(null) }
    val shown = preview?.toLong()?.coerceIn(0, duration) ?: position
    val range = duration.coerceAtLeast(1).toFloat()
    val thickness by animateDpAsState(if (preview == null) 5.dp else 9.dp, tween(140), label = "scrubThickness")
    val progressLabel = stringResource(R.string.playback_progress)
    Slider(
        value = shown.toFloat().coerceIn(0f, range),
        onValueChange = { preview = it },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = progressLabel },
        enabled = seekable,
        valueRange = 0f..range,
        onValueChangeFinished = {
            if (seekable && player?.currentMediaItem?.mediaId == episodeId) preview?.let {
                position = it.toLong().coerceIn(0, duration)
                onSeek(position)
            }
            preview = null
        },
        thumb = {},
        track = { slider ->
            Box(Modifier.fillMaxWidth().height(thickness).clip(RoundedCornerShape(50))
                .background(palette.foreground.copy(alpha = .20f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((slider.value / range).coerceIn(0f, 1f))
                    .background(palette.foreground.copy(alpha = if (preview == null) .78f else 1f)))
            }
        }
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(clock(shown), color = palette.secondary)
        Text(if (duration > 0) "−" + clock(duration - shown) else "—", color = palette.secondary)
    }
}
