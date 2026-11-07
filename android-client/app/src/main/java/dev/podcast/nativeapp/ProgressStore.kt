package dev.podcast.nativeapp

import android.content.Context
import org.json.JSONObject
import java.time.Instant

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("playback-progress", Context.MODE_PRIVATE)
    fun read(id: String): JSONObject? = runCatching { JSONObject(prefs.getString(id,null) ?: return null) }.getOrNull()
    fun record(id: String, position: Long, completed: Boolean): JSONObject {
        val value = JSONObject().put("positionInMilliseconds",position).put("completed",completed).put("recordedAtTimestamp",Instant.now().toString()).put("pending",true)
        prefs.edit().putString(id,value.toString()).apply(); return value
    }
    fun accept(id: String, value: JSONObject) { prefs.edit().putString(id,value.toString()).apply() }
    suspend fun resume(id: String, api: Api, signedIn: Boolean): Long {
        var best = read(id)
        if(signedIn) runCatching { api.playbackPosition(id) }.getOrNull()?.let { remote ->
            val localTime = runCatching { Instant.parse(best?.optString("recordedAtTimestamp")) }.getOrDefault(Instant.EPOCH)
            val remoteTime = runCatching { Instant.parse(remote.optString("recordedAtTimestamp")) }.getOrDefault(Instant.EPOCH)
            if(remoteTime > localTime) { best = remote; accept(id,remote) }
        }
        return if(best?.optBoolean("completed") == true) 0 else best?.optLong("positionInMilliseconds") ?: 0
    }
    suspend fun sync(id: String, value: JSONObject, api: Api) {
        val timestamp = value.getString("recordedAtTimestamp")
        api.savePosition(id,value.getLong("positionInMilliseconds"),value.optBoolean("completed"),timestamp)
        val latest = read(id)
        if(latest?.optString("recordedAtTimestamp") == timestamp) accept(id,latest.put("pending",false))
    }
}
