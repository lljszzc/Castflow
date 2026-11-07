package dev.podcast.nativeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class PodcastItem(val id: String, val type: String, val title: String, val author: String, val artwork: String, val audio: String, val description: String, val duration: Long, val url: String = "")
data class EditorialSection(val id: String, val title: String, val items: List<PodcastItem>)
data class BrowseCategory(val id: String, val title: String, val artwork: String, val route: String, val color: Long = 0xFFE9EDF4, val ink: Long = 0xFF111111)
fun JSONArray.items(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONObject.item(): PodcastItem {
    val a = optJSONObject("attributes") ?: JSONObject()
    return PodcastItem(optString("id"), optString("type"), a.optString("name"), a.optString("artistName"),
        a.optJSONObject("artwork")?.optString("url")?.replace("{w}","600")?.replace("{h}","600")?.replace("{f}","jpg") ?: "",
        a.optString("assetUrl"), a.optJSONObject("description")?.optString("standard") ?: "", a.optLong("durationInMilliseconds"), a.optString("url"))
}
class Api(private val userToken: () -> String?) {
    private val publicCache = object : LinkedHashMap<String,Pair<Long,JSONObject>>(8,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,Pair<Long,JSONObject>>?) = size > 8
    }
    private var regionCheckedAt = 0L
    private var regionUser: String? = null
    private class HttpFailure(val code: Int) : java.io.IOException(if (code == 403) "登录会话不可用，请重新登录" else "请求失败（$code）")
    private var bearer = ""
    var editorialSections = emptyList<EditorialSection>()
        private set
    var homeFailures = emptyList<String>()
        private set
    suspend fun home(signedIn: Boolean): List<EditorialSection> = coroutineScope {
        val failures = java.util.concurrent.CopyOnWriteArrayList<String>()
        val jobs = mutableListOf<kotlinx.coroutines.Deferred<EditorialSection?>>()
        if(signedIn) listOf("新单集" to "library/new-episodes", "继续播放" to "continue-playing/podcast-episodes", "最近播放" to "recent/played/podcast-episodes").forEach { (title,path) ->
            jobs += async { try { val data = request("/v1/me/$path?limit=12",true).optJSONArray("data")?.items()?.map { it.item() } ?: emptyList(); EditorialSection(path,title,data) } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; failures += title; null } }
        }
        jobs += async { try { val r = request("/v1/catalog/$storefront/charts?chart=top&genre=26&types=podcasts&limit=12"); val items = r.optJSONObject("results")?.optJSONArray("podcasts")?.items()?.flatMap { it.optJSONArray("data")?.items() ?: emptyList() }?.map { it.item() } ?: emptyList(); EditorialSection("popular","热门节目",items) } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; failures += "热门节目"; null } }
        val basics = jobs.awaitAll().filterNotNull().filter { it.items.isNotEmpty() }
        val editorial = try { discover(); editorialSections } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; failures += "编辑精选"; emptyList() }
        homeFailures = failures.toList()
        clearPagination()
        uniqueEditorialSections(basics + editorial)
    }
    var storefront = "cn"
    var next: String? = null
        private set
    private var pendingPages = emptyList<String>()
    fun paginationSnapshot(): List<String> = listOfNotNull(next) + pendingPages
    fun restorePagination(paths: List<String>) { cursors(paths) }
    fun clearPagination() { next = null; pendingPages = emptyList() }
    private fun cursors(paths: List<String>) { val all = paths.filter { it.isNotBlank() }.distinct(); next = all.firstOrNull(); pendingPages = all.drop(1) }
    private fun page(data: JSONObject): List<PodcastItem> {
        cursors(listOf(data.optString("next")))
        return data.optJSONArray("data")?.items()?.map { it.item() } ?: emptyList()
    }
    suspend fun more(): List<PodcastItem> {
        val uri = URI(next ?: return emptyList())
        require(uri.host == null || (uri.scheme == "https" && uri.host == "amp-api.podcasts.apple.com"))
        require(uri.path.startsWith("/v1/"))
        val remaining = pendingPages
        val payload = request(uri.rawPath + (uri.rawQuery?.let { "?$it" } ?: ""), uri.path.startsWith("/v1/me/"))
        val items = resultsPage(payload)
        cursors(listOfNotNull(next) + pendingPages + remaining)
        return items
    }
    private fun resultsPage(payload: JSONObject): List<PodcastItem> {
        val results = payload.optJSONObject("results") ?: return page(payload)
        val blocks = results.keys().asSequence().flatMap { key ->
            when(val value = results.opt(key)) { is JSONArray -> value.items().asSequence(); is JSONObject -> sequenceOf(value); else -> emptySequence() }
        }.toList()
        cursors(blocks.map { it.optString("next") })
        return blocks.flatMap { it.optJSONArray("data")?.items() ?: emptyList() }.map { it.item() }
    }
    private fun http(url: String, method: String = "GET", account: Boolean = false, body: JSONObject? = null): String {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host in setOf("podcasts.apple.com", "amp-api.podcasts.apple.com"))
        val c = uri.toURL().openConnection() as HttpURLConnection
        c.instanceFollowRedirects = false; c.connectTimeout = 20000; c.readTimeout = 25000; c.requestMethod = method
        if (uri.host.startsWith("amp-api")) {
            c.setRequestProperty("Authorization", "Bearer $bearer")
            if (account) userToken()?.let { c.setRequestProperty("media-user-token", it) }
            c.setRequestProperty("Origin", "https://podcasts.apple.com")
        }
        try {
            if(body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
            val code = c.responseCode
            if (code !in 200..299) throw HttpFailure(code)
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
    private fun ensureBearer() {
        if (bearer.isNotEmpty()) {
            val expiry = runCatching { JSONObject(String(android.util.Base64.decode(bearer.split('.')[1], android.util.Base64.URL_SAFE))).optLong("exp") }.getOrDefault(0)
            if (expiry > System.currentTimeMillis() / 1000 + 60) return
        }
        val html = http("https://podcasts.apple.com/cn/new")
        val path = Regex("<script[^>]+src=\"(/assets/index[^\"]+\\.js)\"").find(html)?.groupValues?.get(1) ?: error("暂时无法获取服务配置")
        val js = http("https://podcasts.apple.com$path")
        bearer = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").find(js)?.value ?: error("暂时无法获取服务令牌")
    }
    suspend fun request(path: String, account: Boolean = false, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val cacheable = method == "GET" && !account && path.startsWith("/v1/editorial/")
        if(cacheable) synchronized(publicCache) { publicCache[path] }?.let { (time,data) -> if(System.currentTimeMillis()-time < 180_000) return@withContext data }
        ensureBearer()
        require(path.startsWith("/v1/"))
        val text = try { http("https://amp-api.podcasts.apple.com$path", method, account, body) }
        catch (e: HttpFailure) {
            // Retry only reads; repeating a mutation after an ambiguous response
            // could accidentally duplicate an operation.
            if (e.code != 401 || method != "GET") throw e
            bearer = ""; ensureBearer(); http("https://amp-api.podcasts.apple.com$path", method, account)
        }
        val data = if (text.isBlank()) JSONObject() else JSONObject(text)
        if(cacheable) synchronized(publicCache) { publicCache[path] = System.currentTimeMillis() to data }
        data
    }
    suspend fun accountRegion() {
        val user = userToken()
        if(user == regionUser && regionCheckedAt > 0 && System.currentTimeMillis()-regionCheckedAt < 120_000) return
        val rows = request("/v1/me/storefront", true).optJSONArray("data")
        rows?.optJSONObject(0)?.optString("id")?.takeIf { it.matches(Regex("[a-z]{2}")) }?.let { storefront = it }
        regionUser = user; regionCheckedAt = System.currentTimeMillis()
    }
    suspend fun charts(episodes: Boolean = false, subscribers: Boolean = false, genre: String = "26"): List<PodcastItem> {
        val type = if (episodes) "podcast-episodes" else "podcasts"
        require(genre.matches(Regex("\\d+")))
        val data = request("/v1/catalog/$storefront/charts?chart=${if(subscribers) "top-subscriber" else "top"}&genre=$genre&types=$type&limit=30")
        return resultsPage(data)
    }
    suspend fun categories(): List<BrowseCategory> = withContext(Dispatchers.Default) {
        val payload = request("/v1/editorial/$storefront/groupings?name=search-landing&platform=web&l=${contentLanguage()}")
        val result = linkedMapOf<String,BrowseCategory>()
        fun walk(value: Any?) { when(value) {
            is JSONObject -> {
                val a = value.optJSONObject("attributes")
                val link = a?.optJSONObject("link")
                if(link != null && link.optString("label").isNotBlank()) {
                    val uri = android.net.Uri.parse(link.optString("url"))
                    val id = uri.getQueryParameter("id") ?: uri.getQueryParameter("fcId")
                    val route = when {
                        uri.path?.contains("top-charts") == true -> "charts:26"
                        uri.path?.contains("viewGenre") == true && id != null -> "charts:$id"
                        uri.path?.contains("viewGrouping") == true && id != null -> "/v1/editorial/$storefront/groupings/$id"
                        uri.path?.contains("viewMultiRoom") == true && id != null -> "/v1/editorial/$storefront/multirooms/$id"
                        else -> null
                    }
                    if(route != null) {
                        val art = a.optJSONObject("artwork")?.optString("url")?.replace("{w}","800")?.replace("{h}","200")?.replace("{c}","bb")?.replace("{f}","png") ?: ""
                        val artwork = a.optJSONObject("artwork")
                        fun color(key: String, fallback: Long) = runCatching { android.graphics.Color.parseColor("#"+artwork?.optString(key)).toLong() and 0xffffffffL }.getOrDefault(fallback)
                        result[route] = BrowseCategory(value.optString("id"),link.optString("label"),art,route,color("bgColor",0xFFE9EDF4),color("textColor1",0xFF111111))
                    }
                }
                value.keys().forEach { walk(value.opt(it)) }
            }
            is JSONArray -> (0 until value.length()).forEach { walk(value.opt(it)) }
        } }
        walk(payload); result.values.toList()
    }
    suspend fun category(category: BrowseCategory): List<PodcastItem> {
        if(category.route.startsWith("charts:")) return charts(genre = category.route.substringAfter(':'))
        val payload = request(category.route)
        val found = linkedMapOf<String,PodcastItem>()
        fun walk(x: Any?) { when(x) {
            is JSONObject -> { if(x.optString("type") in setOf("podcasts","podcast-episodes") && x.optJSONObject("attributes")?.has("name") == true) { val item = x.item(); found[item.type+item.id] = item }; x.keys().forEach { walk(x.opt(it)) } }
            is JSONArray -> (0 until x.length()).forEach { walk(x.opt(it)) }
        } }
        withContext(Dispatchers.Default) { walk(payload) }; return found.values.toList()
    }
    suspend fun discover(): List<PodcastItem> = withContext(Dispatchers.Default) {
        val payload = request("/v1/editorial/$storefront/groupings?platform=web&name=podcasts&include%5Bgroupings%5D=category&l=${contentLanguage()}")
        val found = linkedMapOf<String, PodcastItem>()
        val sections = linkedMapOf<String,EditorialSection>()
        fun walk(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val contents = value.optJSONObject("relationships")?.optJSONObject("contents")?.optJSONArray("data")?.items()?.filter { it.optString("type") in setOf("podcasts","podcast-episodes") && it.optJSONObject("attributes")?.has("name") == true }?.map { it.item() } ?: emptyList()
                    val title = value.optJSONObject("attributes")?.optString("name") ?: ""
                    if(title.isNotBlank() && contents.isNotEmpty()) sections[value.optString("id")] = EditorialSection(value.optString("id"),title,contents)
                    if (value.optString("type") == "podcasts" && value.optJSONObject("attributes")?.has("name") == true) {
                        val item = value.item(); found.putIfAbsent(item.id, item)
                    }
                    value.keys().forEach { walk(value.opt(it)) }
                }
                is JSONArray -> (0 until value.length()).forEach { walk(value.opt(it)) }
            }
        }
        walk(payload)
        editorialSections = uniqueEditorialSections(sections.values.toList())
        return@withContext found.values.toList()
    }
    suspend fun search(term: String): List<PodcastItem> {
        return resultsPage(request("/v1/catalog/$storefront/search?term=${URLEncoder.encode(term,"UTF-8")}&types=podcasts,podcast-episodes&limit=20"))
    }
    suspend fun library(path: String): List<PodcastItem> = page(request("/v1/me/$path?limit=30", true))
    suspend fun episodes(id: String): List<PodcastItem> = page(request("/v1/catalog/$storefront/podcasts/$id?include=episodes").optJSONArray("data")?.optJSONObject(0)?.optJSONObject("relationships")?.optJSONObject("episodes") ?: JSONObject())
    suspend fun episode(id: String): PodcastItem = request("/v1/catalog/$storefront/podcast-episodes/$id").getJSONArray("data").getJSONObject(0).item()
    suspend fun update(item: PodcastItem, add: Boolean) {
        require(item.id.matches(Regex("\\d+")))
        val kind = if (item.type == "podcasts") "podcasts" else "podcast-episodes"
        request("/v1/me/library/$kind/${item.id}", true, if (add) "POST" else "DELETE")
    }
    suspend fun playbackPosition(id: String): JSONObject? {
        require(id.matches(Regex("\\d+")))
        return request("/v1/me/playback/positions/podcast-episodes?ids=$id", true).optJSONArray("data")?.items()?.firstOrNull { it.optString("id") == id }?.optJSONObject("attributes")
    }
    suspend fun savePosition(id: String, position: Long, completed: Boolean, timestamp: String) {
        require(id.matches(Regex("\\d+")))
        request("/v1/me/playback/positions/podcast-episodes/$id", true, "PUT", JSONObject().put("type","playback-positions").put("attributes", JSONObject().put("positionInMilliseconds",position).put("completed",completed).put("recordedAtTimestamp",timestamp)))
    }
}
