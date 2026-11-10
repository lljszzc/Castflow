package dev.podcast.nativeapp
import androidx.core.net.toUri

import androidx.core.content.edit

import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.chrisbanes.haze.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.sign
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var playerRequest by mutableIntStateOf(0)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.action == "dev.podcast.OPEN_PLAYER") playerRequest++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.action == "dev.podcast.OPEN_PLAYER") playerRequest++
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF008CFF), background = Color.White)) { CompositionLocalProvider(LocalGlassState provides remember { HazeState() }) { PodcastApp(playerRequest) } } }
    }
}
data class CachedPage(val rows: List<PodcastItem>, val shelves: List<EditorialSection>, val categories: List<BrowseCategory>, val warning: String, val cursors: List<String>, val time: Long = android.os.SystemClock.elapsedRealtime())
private object PageMemory {
    private var accountDigest: ByteArray? = null
    val positions = mutableMapOf<String,Pair<Int,Int>>()
    val pages = object : LinkedHashMap<String,CachedPage>(24,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,CachedPage>?) = size > 24
    }
    fun validate(token: String?): Boolean {
        val digest = token?.let { java.security.MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) }
        if (accountDigest.contentEquals(digest)) return false
        pages.clear()
        positions.clear()
        accountDigest = digest
        return true
    }
}
private fun sessionToken(): String? = CookieManager.getInstance().getCookie("https://podcasts.apple.com")?.split(';')?.map { it.trim() }?.firstOrNull { it.startsWith("media-user-token=") }?.substringAfter('=')

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PodcastApp(playerRequest: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { Api(::sessionToken) }
    val listState = rememberLazyListState()
    var compact by remember { mutableStateOf(false) }
    val gestureThreshold = with(LocalDensity.current) { 18.dp.toPx() }
    val scrollIntent = remember(listState,gestureThreshold) { object : NestedScrollConnection {
        var travel = 0f
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if(source == NestedScrollSource.UserInput && abs(available.y) > .5f) {
                if(sign(travel) != sign(available.y)) travel = 0f
                travel += available.y
                if(abs(travel) >= gestureThreshold) {
                    if(travel < 0 && listState.canScrollForward) compact = true
                    if(travel > 0) compact = false
                    travel = 0f
                }
            }
            return Offset.Zero
        }
    } }
    val downloads = remember { Downloads(context.applicationContext) }
    val progress = remember { ProgressStore(context.applicationContext) }
    var signedIn by remember { mutableStateOf(sessionToken() != null) }
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("主页") }
    var section by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("概览") }
    val libraryPreferences = remember { LibraryPreferences(context) }
    var layoutRevision by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<String?>(null) }
    var appSettings by remember { mutableStateOf(false) }
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<PodcastItem?>(null) }
    var episodeInfo by remember { mutableStateOf<PodcastItem?>(null) }
    var rows by remember { mutableStateOf(emptyList<PodcastItem>()) }
    var shelves by remember { mutableStateOf(emptyList<EditorialSection>()) }
    var categories by remember { mutableStateOf(emptyList<BrowseCategory>()) }
    var browseCategory by remember { mutableStateOf<BrowseCategory?>(null) }
    var homeWarning by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var auth by remember { mutableStateOf(false) }
    var miniPlayerBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var fullPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(playerRequest) { if(playerRequest > 0) fullPlayer = true }
    var playerCoversBars by remember { mutableStateOf(false) }
    var queue by remember { mutableStateOf(false) }
    var queueRefresh by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf<PodcastItem?>(null) }
    var player by remember { mutableStateOf<MediaController?>(null) }
    val snackbar = remember { SnackbarHostState() }
    suspend fun SnackbarHostState.showLocalizedSnackbar(message: String) = showSnackbar(uiText(message,context))
    var pendingPlayback by remember { mutableStateOf<PodcastItem?>(null) }
    var playbackRequest by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    fun syncController(p: Player?) {
        playing = p != null && p.playWhenReady && p.playbackState != Player.STATE_ENDED && p.playerError == null
        buffering = p != null && p.playWhenReady && p.playbackState == Player.STATE_BUFFERING
        val media = p?.currentMediaItem
        if (media == null) current = null
        else if (current?.id != media.mediaId) {
            val metadata = media.mediaMetadata
            current = PodcastItem(media.mediaId, "podcast-episodes", metadata.title?.toString().orEmpty(),
                metadata.artist?.toString().orEmpty(), metadata.artworkUri?.toString().orEmpty(), "", "", p.duration.coerceAtLeast(0))
        }
    }
    DisposableEffect(player) {
        val controller = player
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onEvents(p: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                syncController(p)
                if (events.contains(Player.EVENT_TIMELINE_CHANGED)) queueRefresh++
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                buffering = false; playing = false
                scope.launch { snackbar.showLocalizedSnackbar("音频加载失败，请重试") }
            }
        }
        syncController(controller)
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }
    val playbackLoading = pendingPlayback != null || buffering
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var downloadEntries by remember { mutableStateOf(emptyMap<String, Downloads.Entry>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    BackHandler(enabled = (browseCategory != null || detail != null || tab == "排行榜" || (tab == "资料库" && section != "概览")) && !auth && !fullPlayer) { if(detail != null) detail = null else if(browseCategory != null) browseCategory = null else if(tab == "排行榜") { tab = "新发现"; section = "概览" } else section = "概览" }
    val detailColors = rememberArtworkColors(detail?.artwork ?: "")
    val bg by animateColorAsState(if (detail != null) detailColors.background else Color.White, label = "coverBackground")
    val ink = if(bg.luminance() > .179f) Color.Black else Color.White
    val pageMotion = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(tab,section,detail?.id) { pageMotion.snapTo(0f); pageMotion.animateTo(1f,spring(dampingRatio = 1f,stiffness = 500f)) }
    var loadedPage by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        var disposed = false
        future.addListener({
            if (!disposed) try {
                player = future.get()
            } catch (_: java.util.concurrent.ExecutionException) {
                scope.launch { snackbar.showLocalizedSnackbar("播放器正在连接，请重试") }
            }
        }, context.mainExecutor)
        onDispose { disposed = true; MediaController.releaseFuture(future) }
    }
    var handledRefresh by remember { mutableIntStateOf(-1) }
    suspend fun load() {
        val force = handledRefresh != refresh
        handledRefresh = refresh
        error = null
        val cacheAccount = sessionToken()
        if(PageMemory.validate(cacheAccount)) loadedPage = ""
        val pageKey = "${contentLanguage()}/$tab/$section/${detail?.id}/${browseCategory?.id}/$query"
        if (tab == "资料库" && section == "下载" && detail == null && browseCategory == null) {
            if (loadedPage != pageKey) {
                if (loadedPage.isNotEmpty()) PageMemory.positions[loadedPage] = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                val saved = PageMemory.positions[pageKey]
                listState.requestScrollToItem(saved?.first ?: 0, saved?.second ?: 0)
                rows = emptyList()
                compact = false
                loadedPage = pageKey
            }
            shelves = emptyList()
            categories = emptyList()
            hasMore = false
            api.clearPagination()
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    do {
                        val entries = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { downloads.entries() }
                        downloadEntries = entries.associateBy { it.item.id }
                        rows = entries.map { it.item }
                        loading = false
                        if (entries.none { it.active }) break
                        delay(3000)
                    } while (true)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    error = "加载失败，请重试"
                    loading = false
                }
            }
            return
        }
        val cached = PageMemory.pages[pageKey] ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { PageDiskCache(context).read(cacheAccount,pageKey) }?.also { PageMemory.pages[pageKey] = it }
        if(loadedPage != pageKey) {
            if(loadedPage.isNotEmpty()) PageMemory.positions[loadedPage] = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            val savedPosition = PageMemory.positions[pageKey]
            listState.requestScrollToItem(savedPosition?.first ?: 0,savedPosition?.second ?: 0)
            compact = detail != null
            rows = cached?.rows ?: emptyList(); shelves = cached?.shelves ?: emptyList()
            categories = cached?.categories ?: emptyList(); homeWarning = cached?.warning ?: ""
            loadedPage = pageKey
        }
        loading = cached == null && rows.isEmpty()
        if(cached != null && !force && android.os.SystemClock.elapsedRealtime()-cached.time < 60_000L) {
            api.restorePagination(cached.cursors); hasMore = api.next != null; loading = false; return
        }
        hasMore = false; api.clearPagination()
        try {
            if (signedIn) api.accountRegion()
            rows = when {
                detail != null -> api.episodes(detail!!.id)
                browseCategory != null -> api.category(browseCategory!!)
                tab == "主页" -> { shelves = api.home(signedIn); homeWarning = api.homeFailures.joinToString("、"); emptyList() }
                tab == "搜索" && query.isBlank() -> { categories = api.categories(); emptyList() }
                tab == "搜索" -> api.search(query)
                tab == "资料库" && signedIn -> api.library(when (section) { "已保存" -> "library/podcast-episodes"; "最新单集" -> "library/latest-episodes"; "最近播放" -> "recent/played/podcast-episodes"; else -> "library/podcasts" })
                tab == "资料库" -> emptyList()
                tab == "排行榜" -> api.charts(section == "单集", section == "订阅节目")
                tab == "新发现" -> { shelves = api.discovery(); emptyList() }
                else -> api.charts()
            }
            if (detail != null || browseCategory != null || (tab != "新发现" && tab != "主页")) {
                shelves = emptyList()
            }
            hasMore = api.next != null
            if(cached != null) {
                if (tab != "排行榜") rows = updatedFirst(rows,cached.rows) { it.type+":"+it.id to it }
                if (tab != "新发现") shelves = updatedFirst(shelves,cached.shelves) { it.title to it.items }
                categories = updatedFirst(categories,cached.categories) { it.id to it }
            }
            val snapshot = CachedPage(rows,shelves,categories,homeWarning,api.paginationSnapshot())
            PageMemory.pages[pageKey] = snapshot
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { PageDiskCache(context).write(cacheAccount,pageKey,snapshot) }
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = e.message ?: "加载失败，请重试" }
        finally { loading = false }
    }
    var paginationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(tab, section, detail, browseCategory, query, signedIn, refresh) {
        paginationJob?.cancel()
        hasMore = false
        if (tab == "搜索" && query.isNotBlank() && detail == null) delay(350)
        load()
    }
    fun play(item: PodcastItem, enqueue: Boolean = false) {
        val request = if(enqueue) playbackRequest else ++playbackRequest
        if(!enqueue) pendingPlayback = item
        scope.launch {
        try {
            val ep = kotlinx.coroutines.withTimeout(30000) { if (item.audio.isBlank()) api.episode(item.id) else item }
            if(!enqueue && request != playbackRequest) return@launch
            if (ep.audio.isBlank()) { if(!enqueue && request == playbackRequest) pendingPlayback = null; snackbar.showLocalizedSnackbar("本单集未提供可用的公开音频"); return@launch }
            val controller = player ?: run { if(!enqueue && request == playbackRequest) pendingPlayback = null; snackbar.showLocalizedSnackbar("播放器正在连接，请重试"); return@launch }
            val media = MediaItem.Builder().setMediaId(ep.id).setUri(ep.audio).setMediaMetadata(MediaMetadata.Builder().setTitle(ep.title).setArtist(ep.author).setArtworkUri(ep.artwork.toUri()).build()).build()
            if (enqueue) { controller.addMediaItem(media); if(controller.mediaItemCount == 1) controller.prepare(); snackbar.showLocalizedSnackbar("已加入待播列表"); return@launch }
            val resume = kotlinx.coroutines.withTimeout(30000) { progress.resume(ep.id,api,signedIn) }
            if(request != playbackRequest) return@launch
            current = ep
            controller.setMediaItem(media, resume)
            controller.prepare(); controller.setPlaybackSpeed(libraryPreferences.prefs.getFloat("speed",1f)); controller.play()
        } catch (e: Exception) {
            if(e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            if(!enqueue && request == playbackRequest) pendingPlayback = null
            if(enqueue || request == playbackRequest) snackbar.showLocalizedSnackbar(if(e is kotlinx.coroutines.TimeoutCancellationException) "音频加载超时，请重试" else e.message ?: "无法播放")
        } finally { if(!enqueue && request == playbackRequest) pendingPlayback = null }
    } }
    fun update(item: PodcastItem, add: Boolean) { if (!signedIn) { auth = true; return }; scope.launch {
        try { api.update(item, add); snackbar.showLocalizedSnackbar(if (add) "已提交，正在更新资料库" else "已移除"); refresh++ }
        catch (e: Exception) { snackbar.showLocalizedSnackbar(e.message ?: "操作失败") }
    } }
    fun downloadItem(item: PodcastItem) { scope.launch {
        try { if(tab == "资料库" && section == "下载") { downloads.remove(item.id); refresh++ }
        else { val ep = if(item.audio.isBlank()) api.episode(item.id) else item; snackbar.showLocalizedSnackbar(if(downloads.enqueue(ep)) "已开始下载" else "已有下载记录") } }
        catch(e: Exception) { snackbar.showLocalizedSnackbar(e.message ?: "下载失败") }
    } }
    val lightStatusIcons = auth || (!playerCoversBars && bg.luminance() > .179f)
    val lightNavigationIcons = auth || !fullPlayer
    SideEffect {
        val activity = context as? android.app.Activity
        activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window,it.window.decorView).apply { isAppearanceLightStatusBars = lightStatusIcons; isAppearanceLightNavigationBars = lightNavigationIcons } }
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = bg, contentColor = ink,
        bottomBar = {
            BottomDock(compact, tab, pendingPlayback ?: current, playing, playbackLoading,
                onTab = { tab = it; detail = null; browseCategory = null; section = "概览" },
                onExpand = { compact = false }, onPlayer = { fullPlayer = true },
                onToggle = { player?.run { if(playWhenReady) pause() else { if(playbackState == androidx.media3.common.Player.STATE_IDLE) prepare(); play() } } },
                onNext = { player?.seekToNextMediaItem() }, onPlayerBounds = { miniPlayerBounds = it })
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().graphicsLayer { alpha = pageMotion.value; translationX = (1f-pageMotion.value)*36f }.padding(top = padding.calculateTopPadding()).hazeSource(LocalGlassState.current).background(bg).nestedScroll(scrollIntent), state = listState, contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 200.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (browseCategory != null || detail != null || tab == "排行榜" || (tab == "资料库" && section != "概览")) SoftIconButton(onClick = { if (detail != null) detail = null else if(browseCategory != null) browseCategory = null else if(tab == "资料库") section = "概览" else { tab = "新发现"; section = "概览" } }) { LocalizedIcon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
                LocalizedText(if(detail != null) "" else browseCategory?.title ?: tab, Modifier.weight(1f), fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Box {
                    MoreMenuButton(onClick = { menu = true })
                    if(menu) GlassMenu(listOf(
                        "编辑置顶项" to { editor = "编辑置顶项" }, "编辑类别" to { editor = "编辑类别" },
                        "设置" to { appSettings = true }, "账户" to { auth = true }
                    ), { menu = false })
                }
            } }
            if (detail != null) item { val show = detail ?: return@item; Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(show.artwork, show.title, Modifier.fillMaxWidth(.72f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)))
                androidx.compose.material3.Text(show.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                androidx.compose.material3.Text(show.author, fontSize = 20.sp, color = detailColors.secondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SoftIconButton(onClick = { libraryPreferences.togglePin(show); layoutRevision++ }) { LocalizedIcon(if(libraryPreferences.pins().any { it.id == show.id }) Icons.Rounded.PushPin else Icons.Rounded.Add, "置顶") }
                    SoftFilledButton(onClick = { rows.firstOrNull()?.let { play(it) } }, enabled = rows.isNotEmpty() && rows.first().type == "podcast-episodes", modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = bg)) { LocalizedIcon(Icons.Rounded.PlayArrow, null); LocalizedText("播放最新单集", fontSize = 17.sp) }
                    SoftIconButton(onClick = { update(show, true) }) { LocalizedIcon(Icons.Rounded.AddCircleOutline, "关注节目") }
                }
                var expanded by remember(show.id) { mutableStateOf(false) }
                androidx.compose.material3.Text(remember(show.description) { android.text.Html.fromHtml(show.description, 0).toString() }, color = detailColors.secondary, maxLines = if(expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().glassTap { expanded = !expanded })
                Row { SoftTextButton(onClick = { update(show, false) }) { LocalizedText("取消关注", color = ink) }; SoftTextButton(onClick = { expanded = !expanded }) { LocalizedText(if(expanded) "收起简介" else "更多", color = ink) } }
                LocalizedText("单集", Modifier.align(Alignment.Start), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } }
            if (tab == "搜索" && detail == null && browseCategory == null) item { OutlinedTextField(query, { query = it; browseCategory = null }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color(0xFFF0F1F4), unfocusedContainerColor = Color(0xFFF0F1F4)), placeholder = { LocalizedText("节目、单集与创作者") }, leadingIcon = { LocalizedIcon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
            if (tab == "排行榜" && detail == null) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("节目", "单集", "订阅节目").forEach { label -> SoftTextButton(onClick = { section = label }, modifier = Modifier.background(if(section == label) Color(0xFFE7F3FF) else Color.Transparent,RoundedCornerShape(22.dp))) { LocalizedText(label,fontWeight = if(section == label) FontWeight.SemiBold else FontWeight.Normal) } }
            } }
            if(tab == "主页" && detail == null && homeWarning.isNotBlank()) item { LocalizedText("暂未载入：$homeWarning",color = Color.Gray,fontSize = 13.sp); SoftTextButton(onClick = { refresh++ }) { LocalizedText("重试") } }
            if (tab == "新发现" && detail == null && shelves.isEmpty()) item { LocalizedText("编辑精选", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { message -> item { LocalizedText(message, color = MaterialTheme.colorScheme.error); SoftTextButton(onClick = { refresh++ }) { LocalizedText("重试") } } }
            if (!loading && rows.isEmpty() && shelves.isEmpty() && error == null && !(tab == "搜索" && query.isBlank() && categories.isNotEmpty())) item { Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LocalizedIcon(Icons.Rounded.Headphones, null, Modifier.size(54.dp), tint = Color.Gray)
                LocalizedText(when { tab == "资料库" && section != "下载" && !signedIn -> "登录后查看你的 Apple 播客资料库"; tab == "搜索" -> "搜索你感兴趣的声音"; else -> "这里暂时没有内容" }, Modifier.padding(top = 16.dp))
                if (!signedIn && tab == "资料库" && section != "下载") SoftFilledButton(onClick = { auth = true }) { LocalizedText("登录") }
            } }
            if(tab == "搜索" && query.isBlank() && browseCategory == null && detail == null) {
                item { LocalizedText("浏览分类",fontSize = 24.sp,fontWeight = FontWeight.Bold) }
                items(categories.chunked(2)) { group -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEach { category -> Box(Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(16.dp)).background(Color(category.color)).glassTap { browseCategory = category }) {
                        AsyncImage(category.artwork,null,Modifier.fillMaxHeight().fillMaxWidth(.5f).align(Alignment.CenterEnd),contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        LocalizedText(category.title,Modifier.align(Alignment.BottomStart).fillMaxWidth(.7f).padding(12.dp),fontWeight = FontWeight.Bold,fontSize = 17.sp,color = Color.White,maxLines = 2)
                    } }; if(group.size == 1) Spacer(Modifier.weight(1f))
                } }
            } else if((tab == "主页" || tab == "新发现") && detail == null && shelves.isNotEmpty()) {
                items(shelves, key = { it.id }) { shelf -> Column(Modifier.animateItem(fadeInSpec = tween(180),fadeOutSpec = tween(120),placementSpec = tween(220)),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val chart = DiscoveryChart.entries.firstOrNull { it.shelfId == shelf.id }
                    if (chart == null) {
                        LocalizedText(shelf.title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(Modifier.fillMaxWidth().glassTap { section = chart.section; tab = "排行榜" }.heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            LocalizedText(chart.title, Modifier.weight(1f), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            LocalizedIcon(Icons.Rounded.ChevronRight, null)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(shelf.items, key = { it.id }) { entry -> Column(Modifier.width(155.dp).glassTap { if(entry.type == "podcasts") detail = entry else episodeInfo = entry }) {
                            AsyncImage(entry.artwork,entry.title,Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)))
                            androidx.compose.material3.Text(entry.title,Modifier.padding(top = 8.dp), maxLines = 2,overflow = TextOverflow.Ellipsis)
                            androidx.compose.material3.Text(entry.author,maxLines = 1,overflow = TextOverflow.Ellipsis,color = Color.Gray,fontSize = 13.sp)
                        } }
                    }
                } }
            } else if(tab == "资料库" && section == "概览" && detail == null) {
                item { LibraryHubHeader(libraryPreferences, layoutRevision, { section = it }, { detail = it }) }
                items(rows.chunked(2)) { group -> LibraryGridRow(group) { detail = it } }
            } else items(rows, key = { it.type + it.id }, contentType = { it.type }) { item ->
                Column(Modifier.animateItem(fadeInSpec = tween(180),fadeOutSpec = tween(120),placementSpec = tween(240))) { Row(Modifier.fillMaxWidth().glassTap { if (item.type == "podcasts") detail = item else episodeInfo = item }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(item, 70)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { androidx.compose.material3.Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); androidx.compose.material3.Text(item.author, fontSize = 13.sp, color = if(detail != null) detailColors.secondary else Color.Gray, maxLines = 1) }
                    if (item.type != "podcasts") SoftIconButton(onClick = { play(item) }) { LocalizedIcon(Icons.Rounded.PlayCircle, "播放", tint = if(detail != null) ink else MaterialTheme.colorScheme.primary) }
                    if(item.type != "podcasts") Box {
                        var actions by remember(item.id) { mutableStateOf(false) }
                        SoftIconButton(onClick = { actions = true }) { LocalizedIcon(Icons.Rounded.MoreVert, "单集操作", tint = ink) }
                        if(actions) GlassMenu(listOf(
                            "保存单集" to { update(item,true) }, "稍后播放" to { play(item,true) },
                            (if(tab == "资料库" && section == "下载") "删除下载" else "下载") to { downloadItem(item) },
                            "取消保存" to { update(item,false) }
                        ), { actions = false })
                    }
                }
                if(tab == "资料库" && section == "下载") downloadEntries[item.id]?.let { entry -> LocalizedText(entry.status, color = Color.Gray, fontSize = 13.sp)
                    if(entry.retryable) SoftTextButton(onClick = { runCatching { downloads.retry(item) }.onFailure { scope.launch { snackbar.showLocalizedSnackbar("无法重试，请删除后重新下载") } }; refresh++ }) { LocalizedText("重试下载") }
                }
                HorizontalDivider(color = Color.Black.copy(alpha = .05f)) }
            }
            if (hasMore) item { SoftTextButton(enabled = !loading, onClick = { if (paginationJob?.isActive != true) paginationJob = scope.launch {
                loading = true
                try { rows = (rows + api.more()).distinctBy { it.type + it.id }; hasMore = api.next != null; PageMemory.pages[loadedPage] = CachedPage(rows,shelves,categories,homeWarning,api.paginationSnapshot()) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; snackbar.showLocalizedSnackbar(e.message ?: "加载下一页失败") }
                finally { loading = false }
            } }) { LocalizedText("加载更多") } }
        }
    }
    episodeInfo?.let { item -> EpisodeDetails(item, api, { episodeInfo = null }, { play(it) }, { update(it,true) }, { play(it,true) }, { downloadItem(it) }, { ep, complete ->
        if(!signedIn) auth = true else scope.launch {
            try { api.savePosition(ep.id, if(complete) ep.duration.coerceAtLeast(0) else 0, complete, java.time.Instant.now().toString()); snackbar.showLocalizedSnackbar("播放状态已更新"); refresh++ }
            catch(e: Exception) { snackbar.showLocalizedSnackbar(e.message ?: "更新失败") }
        }
    }) }
    editor?.let { action -> LibraryEditor(libraryPreferences, action == "编辑置顶项", { editor = null }) { layoutRevision++; editor = null } }
    if(appSettings) SoftDialog(onDismissRequest = { appSettings = false }, title = { LocalizedText("设置") }) { closeWith -> Column {
        LocalizedText("账户地区：${api.storefront.uppercase()}")
        var wifiOnly by remember { mutableStateOf(libraryPreferences.prefs.getBoolean("wifiOnly",true)) }
        Row(verticalAlignment = Alignment.CenterVertically) { LocalizedText("仅 Wi-Fi 下载", Modifier.weight(1f)); Switch(wifiOnly, { wifiOnly = it; libraryPreferences.prefs.edit { putBoolean("wifiOnly",it) } }) }
        LocalizedText("默认播放速度")
        Row { listOf(1f,1.25f,1.5f,2f).forEach { speed -> SoftTextButton(onClick = { libraryPreferences.prefs.edit { putFloat("speed",speed) }; player?.setPlaybackSpeed(speed) }) { LocalizedText("${speed}×") } } }

        LocalizedText("下载保存在本机；置顶与类别设置在本机保存。")
        SoftTextButton(onClick = { closeWith { appSettings = false; tab = "资料库"; section = "下载" } }) { LocalizedText("管理下载") }
        SoftTextButton(onClick = { closeWith { appSettings = false; auth = true } }) { LocalizedText("管理账户") }
    } }
    AnimatedVisibility(auth,enter = fadeIn(tween(180)) + slideInVertically(tween(260,easing = MotionTokens.enter)) { it/12 },exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it/12 }) { AccountScreen(signedIn,api.storefront,api,::sessionToken, { auth = false }, { signedIn = true; auth = false; refresh++ }, {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            signedIn = false
            api.storefront = "cn"
            rows = emptyList()
            auth = false
            refresh++
        }
    }) }
    if(fullPlayer) (pendingPlayback ?: current)?.let { displayed -> FullPlayer(displayed, playing, playbackLoading, player,
        onDismiss = { fullPlayer = false }, onToggle = { player?.run { if(playWhenReady) pause() else { if(playbackState == androidx.media3.common.Player.STATE_IDLE) prepare(); play() } } },
        onSeek = { player?.seekTo(it) }, onSpeed = { player?.setPlaybackSpeed(it) },
        onQueue = { fullPlayer = false; queue = true }, onSave = { if (!signedIn) fullPlayer = false; update(displayed, true) },
        sourceBounds = miniPlayerBounds, onSleep = { PlaybackService.active?.setSleep(it) }, onStatusCoverage = { playerCoversBars = it }) }
    if (queue) SoftSheet(onDismissRequest = { queue = false }) {
        val controller = player
        val entries = remember(queueRefresh, controller?.mediaItemCount) { (0 until (controller?.mediaItemCount ?: 0)).map { controller!!.getMediaItemAt(it) } }
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { LocalizedText("待播列表", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            items(entries.size) { index -> Row(verticalAlignment = Alignment.CenterVertically) {
                LocalizedText(entries[index].mediaMetadata.title?.toString() ?: "单集", Modifier.weight(1f).glassTap { controller?.seekToDefaultPosition(index); controller?.play(); queue = false }, maxLines = 2)
                SoftIconButton(onClick = { controller?.moveMediaItem(index,index-1); queueRefresh++ }, enabled = index > 0) { LocalizedIcon(Icons.Rounded.ArrowUpward, "上移") }
                SoftIconButton(onClick = { controller?.removeMediaItem(index); queueRefresh++ }) { LocalizedIcon(Icons.Rounded.Close, "移除") }
            } }
            if (entries.isEmpty()) item { LocalizedText("尚无待播单集") }
        }
    }
    }
}
@Composable private fun Artwork(item: PodcastItem, size: Int) { AsyncImage(item.artwork, item.title, Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(Color.LightGray)) }
@OptIn(ExperimentalHazeApi::class)
@Composable internal fun Glass(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val style = HazeDefaults.style(backgroundColor = Color.White, tint = HazeTint(Color.White.copy(alpha = .48f)), blurRadius = 25.dp, noiseFactor = 0f)
    Box(modifier.shadow(8.dp,RoundedCornerShape(32.dp),clip = false,ambientColor = Color.Black.copy(alpha = .08f),spotColor = Color.Black.copy(alpha = .10f)).clip(RoundedCornerShape(32.dp)).hazeEffect(LocalGlassState.current, style) { inputScale = HazeInputScale.Fixed(.8f) }.background(Color.White.copy(alpha = .04f)).then(Modifier), contentAlignment = Alignment.Center) {
        Surface(color = Color.Transparent, shape = RoundedCornerShape(32.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .7f))) { content() }
    }
}
