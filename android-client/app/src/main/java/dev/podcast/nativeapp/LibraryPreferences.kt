package dev.podcast.nativeapp

import androidx.core.content.edit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class LibraryPreferences(context: Context) {
    val prefs = context.getSharedPreferences("library-layout", Context.MODE_PRIVATE)
    private val defaults = listOf("节目", "已保存", "最新单集", "最近播放", "下载")
    fun categories(): List<String> = runCatching { JSONArray(prefs.getString("categories", null)).let { a -> (0 until a.length()).map { a.getString(it) }.filter { it in defaults } } }.getOrDefault(defaults)
    fun categories(values: List<String>) { prefs.edit { putString("categories", JSONArray(values).toString()) } }
    fun pins(): List<PodcastItem> = runCatching { JSONArray(prefs.getString("pins", "[]")).items().map { a -> PodcastItem(a.getString("id"), "podcasts", a.getString("title"), a.getString("author"), a.getString("artwork"), "", a.optString("description"), 0) } }.getOrDefault(emptyList())
    fun pins(values: List<PodcastItem>) { prefs.edit { putString("pins", JSONArray().apply { values.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("author",it.author).put("artwork",it.artwork).put("description",it.description)) } }.toString()) } }
    fun togglePin(item: PodcastItem) { val current = pins(); pins(if (current.any { it.id == item.id }) current.filterNot { it.id == item.id } else current + item) }
}
@Composable fun LibraryHubHeader(preferences: LibraryPreferences, revision: Int, onCategory: (String) -> Unit, onShow: (PodcastItem) -> Unit) {
    val pins = remember(revision) { preferences.pins() }
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        pins.chunked(3).forEach { group -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            group.forEach { item -> Column(Modifier.weight(1f).glassTap { onShow(item) }) {
                AsyncImage(item.artwork, item.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
                androidx.compose.material3.Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
            } }; repeat(3-group.size) { Spacer(Modifier.weight(1f)) }
        } }
        remember(revision) { preferences.categories() }.forEach { name ->
            Row(Modifier.fillMaxWidth().glassTap { onCategory(name) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LocalizedIcon(when(name) { "已保存" -> Icons.Rounded.BookmarkBorder; "下载" -> Icons.Rounded.Download; "最近播放" -> Icons.Rounded.History; "最新单集" -> Icons.Rounded.NewReleases; else -> Icons.Rounded.LibraryMusic }, null, Modifier.size(28.dp))
                LocalizedText(name, Modifier.weight(1f).padding(start = 16.dp), fontSize = 22.sp)
                LocalizedIcon(Icons.Rounded.ChevronRight, null, tint = Color.Gray)
            }
            HorizontalDivider(Modifier.padding(start = 44.dp), color = Color.Black.copy(alpha = .12f))
        }
        LocalizedText("最近添加", Modifier.padding(top = 20.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable fun LibraryGridRow(group: List<PodcastItem>, onShow: (PodcastItem) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        group.forEach { item -> Column(Modifier.weight(1f).glassTap { onShow(item) }) {
            AsyncImage(item.artwork, item.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            androidx.compose.material3.Text(item.title, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
            androidx.compose.material3.Text(item.author, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = Color.Gray)
        } }; if(group.size == 1) Spacer(Modifier.weight(1f))
    }
}
@Composable fun LibraryEditor(preferences: LibraryPreferences, pins: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var selected by remember { mutableStateOf(preferences.categories()) }
    var pinned by remember { mutableStateOf(preferences.pins()) }
    SoftDialog(onDismissRequest = onDismiss, title = { LocalizedText(if (pins) "编辑置顶项" else "编辑类别") },
        showCancel = true, onConfirm = {
            if (pins) preferences.pins(pinned) else preferences.categories(selected)
            onSaved()
        }) {
        Column {
            if(pins && pinned.isEmpty()) LocalizedText("在节目详情中点“置顶”，即可加入此处。")
            val labels = if(pins) pinned.map { it.title } else selected + listOf("节目", "已保存", "最新单集", "最近播放", "下载").filterNot { it in selected }
            labels.forEachIndexed { index, label -> Row(verticalAlignment = Alignment.CenterVertically) {
                LocalizedText(label, Modifier.weight(1f), maxLines = 1)
                if(pins) {
                    SoftIconButton(onClick = { if(index > 0) pinned = pinned.toMutableList().apply { add(index-1, removeAt(index)) } }, enabled = index > 0) { LocalizedIcon(Icons.Rounded.ArrowUpward, "上移") }
                    SoftIconButton(onClick = { pinned = pinned.filterIndexed { i, _ -> i != index } }) { LocalizedIcon(Icons.Rounded.Close, "取消置顶") }
                } else {
                    SoftIconButton(onClick = { selected = selected.toMutableList().apply { add(index-1,removeAt(index)) } }, enabled = index > 0 && label in selected) { LocalizedIcon(Icons.Rounded.ArrowUpward, "上移类别") }
                    Checkbox(label in selected, { checked -> selected = if(checked) selected + label else selected - label })
                }
            } }
        }
    }
}
