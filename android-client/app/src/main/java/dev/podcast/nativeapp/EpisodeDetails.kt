package dev.podcast.nativeapp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EpisodeDetails(initial: PodcastItem, api: Api, onDismiss: () -> Unit, onPlay: (PodcastItem) -> Unit,
    onSave: (PodcastItem) -> Unit, onQueue: (PodcastItem) -> Unit, onDownload: (PodcastItem) -> Unit, onMark: (PodcastItem, Boolean) -> Unit) {
    var item by remember(initial.id) { mutableStateOf(initial) }
    var error by remember(initial.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(initial.id) { try { val details = api.episode(initial.id)
        item = if (initial.audio.startsWith("content://")) details.copy(audio = initial.audio) else details } catch(e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = "详细介绍暂时无法加载" } }
    val colors = rememberArtworkColors(item.artwork)
    val context = LocalContext.current
    SoftSheet(onDismissRequest = onDismiss, containerColor = colors.background, contentColor = colors.foreground) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(item.artwork,item.title, Modifier.fillMaxWidth(.55f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).align(Alignment.CenterHorizontally))
            androidx.compose.material3.Text(item.title,fontSize = 26.sp,fontWeight = FontWeight.Bold)
            androidx.compose.material3.Text(item.author,color = colors.secondary)
            if(item.duration > 0) LocalizedText("${item.duration / 60000} 分钟",color = colors.secondary)
            SoftFilledButton(onClick = { onPlay(item); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = colors.foreground,contentColor = colors.background)) { LocalizedText("播放单集") }
            Row { SoftTextButton(onClick = { onSave(item) }) { LocalizedText("保存",color = colors.foreground) }; SoftTextButton(onClick = { onQueue(item) }) { LocalizedText("稍后播放",color = colors.foreground) }; SoftTextButton(onClick = { onDownload(item) }) { LocalizedText("下载",color = colors.foreground) } }
            Row { SoftTextButton(onClick = { onMark(item,true) }) { LocalizedText("标为已播放",color = colors.foreground) }; SoftTextButton(onClick = { onMark(item,false) }) { LocalizedText("标为未播放",color = colors.foreground) } }
            SoftTextButton(enabled = item.url.startsWith("https://podcasts.apple.com/"), onClick = {
                val url = item.url
                context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,"${item.title}\n$url"),"分享单集"))
            }) { LocalizedText("分享",color = colors.foreground) }
            error?.let { LocalizedText(it) }
            androidx.compose.material3.Text(remember(item.description) { android.text.Html.fromHtml(item.description,0).toString() },fontSize = 16.sp)
        }
    }
}
