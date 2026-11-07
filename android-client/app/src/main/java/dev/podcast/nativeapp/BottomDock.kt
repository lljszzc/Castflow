package dev.podcast.nativeapp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable fun BottomDock(compact: Boolean, tab: String, current: PodcastItem?, playing: Boolean, loading: Boolean,
    onTab: (String) -> Unit, onExpand: () -> Unit, onPlayer: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onPlayerBounds: (androidx.compose.ui.geometry.Rect) -> Unit) {
    val tabs = listOf("主页" to Icons.Rounded.Home, "新发现" to Icons.Rounded.Explore, "资料库" to Icons.Rounded.LibraryMusic, "搜索" to Icons.Rounded.Search)
    val motion by animateFloatAsState(if(compact) 1f else 0f,spring(dampingRatio = 1f,stiffness = 340f),label = "dockMorph")
    val p = motion.coerceIn(0f,1f)
    val lift = dockBottomLift()
    val selected by animateFloatAsState(tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0).toFloat(),spring(1f,420f),label = "tabSelection")
    val expanded = if(current != null) 107.dp else 56.dp
    BoxWithConstraints(Modifier.padding(horizontal = 16.dp).padding(bottom = lift).fillMaxWidth().height(lerp(expanded,43.dp,p))) {
        val width = maxWidth
        val navWidth = lerp(width,43.dp,p)
        val navHeight = lerp(56.dp,43.dp,p)
        val navY = lerp(if(current != null) 51.dp else 0.dp,0.dp,p)
        if(current != null) Glass(Modifier.offset(x = lerp(0.dp,51.dp,p)).width(lerp(width,width-102.dp,p)).height(43.dp).onGloballyPositioned { onPlayerBounds(it.boundsInRoot()) }.glassTap(onPlayer)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 11.dp),verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(current.artwork,null,Modifier.size(29.dp).clip(RoundedCornerShape(7.dp)))
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    androidx.compose.material3.Text(current.title,color = Color.Black,fontSize = 13.sp,lineHeight = 16.sp,maxLines = 1,overflow = TextOverflow.Ellipsis)
                    androidx.compose.material3.Text(current.author,color = Color.Gray,fontSize = 11.sp,lineHeight = 14.sp,maxLines = 1,overflow = TextOverflow.Ellipsis)
                }
                SoftIconButton(onToggle,Modifier.size(34.dp),enabled = !loading) { if(loading) PlaybackLoading(22.dp,Color.Black) else LocalizedIcon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(playing) "暂停" else "播放",Modifier.size(25.dp),tint = Color.Black) }
                SoftIconButton(onNext,Modifier.size(32.dp)) { LocalizedIcon(Icons.Rounded.SkipNext,"下一集",Modifier.size(24.dp),tint = Color.Black) }
            }
        }
        Glass(Modifier.offset(y = navY).width(navWidth).height(navHeight)) {
            Box(Modifier.fillMaxSize()) {
                if(p < .65f) {
                    val cell = navWidth/4
                    Box(Modifier.offset(x = cell*selected).padding(4.dp).width(cell-8.dp).fillMaxHeight().graphicsLayer { alpha = (1f-p*2).coerceAtLeast(0f) }.clip(RoundedCornerShape(30.dp)).background(Color.Black.copy(alpha = .10f)))
                    Row(Modifier.fillMaxSize().graphicsLayer { alpha = (1f-p*2).coerceAtLeast(0f) }) {
                        tabs.forEach { (name,icon) -> Column(Modifier.weight(1f).fillMaxHeight().glassTap { onTab(name) },verticalArrangement = Arrangement.Center,horizontalAlignment = Alignment.CenterHorizontally) {
                            val tint = if(tab == name) Color(0xFF008CFF) else Color.Black
                            LocalizedIcon(icon,name,Modifier.size(23.dp),tint = tint)
                            LocalizedText(name,fontSize = 10.sp,lineHeight = 13.sp,color = tint)
                        } }
                    }
                } else Box(Modifier.fillMaxSize().glassTap(onExpand),contentAlignment = Alignment.Center) {
                    LocalizedIcon(tabs.firstOrNull { it.first == tab }?.second ?: Icons.Rounded.Home,"展开导航",Modifier.size(24.dp).graphicsLayer { alpha = ((p-.5f)*2).coerceIn(0f,1f) },tint = Color(0xFF008CFF))
                }
            }
        }
        if(p > .05f) Glass(Modifier.offset(x = width-43.dp,y = navY).size(43.dp).graphicsLayer { alpha = p; scaleX = .85f+.15f*p; scaleY = scaleX }.glassTap { onTab("搜索") }) {
            Box(Modifier.fillMaxSize(),contentAlignment = Alignment.Center) { LocalizedIcon(Icons.Rounded.Search,"搜索",Modifier.size(24.dp),tint = Color.Black) }
        }
    }
}
