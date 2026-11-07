package dev.podcast.nativeapp

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONObject

/** Public episode downloads, owned by this app and persisted across restarts. */
class Downloads(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("episode-downloads", Context.MODE_PRIVATE)
    fun enqueue(item: PodcastItem): Boolean {
        require(item.id.matches(Regex("\\d+")))
        val uri = Uri.parse(item.audio)
        require(uri.scheme == "https") { "此音频暂不支持安全下载" }
        if (prefs.contains(item.id)) return false
        val request = DownloadManager.Request(uri).setTitle(item.title).setDescription(item.author)
            .setAllowedOverMetered(!context.getSharedPreferences("library-layout", Context.MODE_PRIVATE).getBoolean("wifiOnly",true))
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_PODCASTS, item.id + ".audio")
        val id = manager.enqueue(request)
        val record = JSONObject().put("downloadId", id).put("title", item.title).put("author", item.author).put("artwork", item.artwork).put("duration", item.duration).put("audio",item.audio)
        prefs.edit().putString(item.id, record.toString()).apply()
        return true
    }
    fun completed(): List<PodcastItem> = prefs.all.mapNotNull { (id, raw) ->
        runCatching {
            val record = JSONObject(raw as String)
            val downloadId = record.getLong("downloadId")
            manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) return@runCatching null
                val uri = manager.getUriForDownloadedFile(downloadId) ?: return@runCatching null
                PodcastItem(id, "podcast-episodes", record.getString("title"), record.getString("author"), record.optString("artwork"), uri.toString(), "", record.optLong("duration"))
            }
        }.getOrNull()
    }
    fun status(id: String): String {
        val record = runCatching { JSONObject(prefs.getString(id,null) ?: return "无下载记录") }.getOrNull() ?: return "记录不可用"
        manager.query(DownloadManager.Query().setFilterById(record.getLong("downloadId"))).use { c ->
            if(!c.moveToFirst()) return "下载记录已失效"
            val state = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0)
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return when(state) { DownloadManager.STATUS_SUCCESSFUL -> "可离线播放"; DownloadManager.STATUS_FAILED -> "下载失败，可重试"; DownloadManager.STATUS_PAUSED -> "已暂停，等待网络"; DownloadManager.STATUS_PENDING -> "等待下载"; else -> if(total > 0) "下载中 ${done * 100 / total}%" else "下载中 ${done / 1024} KB" }
        }
    }
    fun entries(): List<PodcastItem> {
        val ready = completed().associateBy { it.id }
        return prefs.all.mapNotNull { (id,raw) -> ready[id] ?: runCatching { val a = JSONObject(raw as String); PodcastItem(id,"podcast-episodes",a.getString("title"),a.getString("author"),a.optString("artwork"),a.optString("audio"),"",a.optLong("duration")) }.getOrNull() }
    }
    fun retry(item: PodcastItem) { require(item.audio.startsWith("https://")); remove(item.id); enqueue(item) }
    fun remove(id: String) {
        val raw = prefs.getString(id, null) ?: return
        manager.remove(JSONObject(raw).getLong("downloadId"))
        prefs.edit().remove(id).apply()
    }
}
