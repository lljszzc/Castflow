package dev.podcast.nativeapp
import androidx.core.net.toUri

import androidx.core.content.edit

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import org.json.JSONObject

/** Public episode downloads, owned by this app and persisted across restarts. */
class Downloads(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("episode-downloads", Context.MODE_PRIVATE)
    fun enqueue(item: PodcastItem): Boolean {
        require(item.id.matches(Regex("\\d+")))
        val uri = item.audio.toUri()
        require(uri.scheme == "https") { "此音频暂不支持安全下载" }
        if (prefs.contains(item.id)) return false
        val request = DownloadManager.Request(uri).setTitle(item.title).setDescription(item.author)
            .setAllowedOverMetered(!context.getSharedPreferences("library-layout", Context.MODE_PRIVATE).getBoolean("wifiOnly",true))
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_PODCASTS, item.id + ".audio")
        val id = manager.enqueue(request)
        val record = JSONObject().put("downloadId", id).put("title", item.title).put("author", item.author).put("artwork", item.artwork).put("duration", item.duration).put("audio",item.audio)
        prefs.edit { putString(item.id, record.toString()) }
        return true
    }
    data class Entry(val item: PodcastItem, val status: String, val active: Boolean, val retryable: Boolean)

    fun entries(): List<Entry> {
        val records = prefs.all.mapNotNull { (id, raw) ->
            runCatching {
                val record = JSONObject(raw as String)
                val item = PodcastItem(id, "podcast-episodes", record.getString("title"), record.getString("author"),
                    record.optString("artwork"), record.optString("audio"), "", record.optLong("duration"))
                record.getLong("downloadId") to item
            }.getOrNull()
        }.toMap()
        if (records.isEmpty()) return emptyList()
        val entries = mutableMapOf<Long, Entry>()
        // DownloadManager limits this query to the calling app's downloads.
        manager.query(DownloadManager.Query()).use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val statusColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val doneColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val item = records[id] ?: continue
                val status = cursor.getInt(statusColumn)
                val localUri = if (status == DownloadManager.STATUS_SUCCESSFUL) manager.getUriForDownloadedFile(id) else null
                entries[id] = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> if (localUri != null)
                        Entry(item.copy(audio = localUri.toString()), "可离线播放", false, false)
                        else Entry(item, "下载记录已失效", false, true)
                    DownloadManager.STATUS_FAILED -> Entry(item, "下载失败，可重试", false, true)
                    DownloadManager.STATUS_PAUSED -> Entry(item, "已暂停，等待网络", true, false)
                    DownloadManager.STATUS_PENDING -> Entry(item, "等待下载", true, false)
                    else -> {
                        val total = cursor.getLong(totalColumn)
                        val done = cursor.getLong(doneColumn).coerceAtLeast(0)
                        val label = if (total > 0) "下载中 ${done * 100 / total}%" else "下载中 ${done / 1024} KB"
                        Entry(item, label, true, false)
                    }
                }
            }
        }
        return records.map { (id, item) -> entries[id] ?: Entry(item, "下载记录已失效", false, true) }
    }
    fun retry(item: PodcastItem) { require(item.audio.startsWith("https://")); remove(item.id); enqueue(item) }
    fun remove(id: String) {
        val raw = prefs.getString(id, null) ?: return
        manager.remove(JSONObject(raw).getLong("downloadId"))
        prefs.edit { remove(id) }
    }
}
