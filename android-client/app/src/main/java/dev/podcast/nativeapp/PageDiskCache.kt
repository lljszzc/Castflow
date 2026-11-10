package dev.podcast.nativeapp

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Display data only, scoped by a one-way account digest. No credentials or audio URLs. */
class PageDiskCache(context: Context) {
    private val directory = File(context.cacheDir,"pages-v1").apply { mkdirs() }
    private fun file(account: String?, page: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(((account ?: "anonymous")+"\u0000"+page).toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory,hash+".json")
    }
    fun read(account: String?, page: String): CachedPage? = runCatching {
        val f = file(account,page)
        if(!f.exists() || f.length() > 4_000_000 || System.currentTimeMillis()-f.lastModified() > 7*86400000L) return null
        decode(JSONObject(f.readText()))
    }.getOrNull()
    fun write(account: String?, page: String, snapshot: CachedPage) {
        runCatching {
            val bytes = encode(snapshot).toString().toByteArray()
            if(bytes.size > 4_000_000) return
            val atomic = AtomicFile(file(account,page))
            val stream = atomic.startWrite()
            try { stream.write(bytes); atomic.finishWrite(stream) } catch(e: Exception) { atomic.failWrite(stream); throw e }
            directory.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() }?.drop(24)?.forEach { it.delete() }
        }
    }
    companion object {
        private fun item(i: PodcastItem) = JSONObject().put("id",i.id).put("type",i.type).put("title",i.title).put("author",i.author).put("artwork",i.artwork).put("description",i.description.take(12000)).put("duration",i.duration).put("url",i.url)
        private fun items(values: List<PodcastItem>) = JSONArray().apply { values.forEach { put(item(it)) } }
        private fun decodeItems(a: JSONArray?): List<PodcastItem> = a?.items()?.map { PodcastItem(it.getString("id"),it.getString("type"),it.optString("title"),it.optString("author"),it.optString("artwork"),"",it.optString("description"),it.optLong("duration"),it.optString("url")) } ?: emptyList()
        internal fun encode(p: CachedPage) = JSONObject().put("rows",items(p.rows)).put("warning",p.warning)
            .put("shelves",JSONArray().apply { p.shelves.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("items",items(it.items))) } })
            .put("categories",JSONArray().apply { p.categories.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("artwork",it.artwork).put("route",it.route).put("color",it.color)) } })
        internal fun decode(p: JSONObject) = CachedPage(decodeItems(p.optJSONArray("rows")),p.optJSONArray("shelves")?.items()?.map { EditorialSection(it.getString("id"),it.getString("title"),decodeItems(it.optJSONArray("items"))) } ?: emptyList(),p.optJSONArray("categories")?.items()?.map { BrowseCategory(it.getString("id"),it.getString("title"),it.optString("artwork"),it.getString("route"),it.optLong("color", 0xFFE9EDF4)) } ?: emptyList(),p.optString("warning"),emptyList(),-60_001L)
    }
}
fun <T,V> updatedFirst(fresh: List<T>, old: List<T>, identity: (T) -> Pair<String,V>): List<T> {
    if (fresh.size < 2 || old.isEmpty()) return fresh
    val previous = old.associate { identity(it) }
    val (changed, unchanged) = fresh.partition {
        val (key, value) = identity(it)
        previous[key] != value
    }
    return changed + unchanged
}
