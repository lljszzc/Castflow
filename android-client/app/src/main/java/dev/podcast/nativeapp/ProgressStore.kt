package dev.podcast.nativeapp

import androidx.core.content.edit

import android.content.Context
import org.json.JSONObject
import java.time.Instant

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("playback-progress", Context.MODE_PRIVATE)
    private fun read(id: String): JSONObject? = runCatching { JSONObject(prefs.getString(id,null) ?: return null) }.getOrNull()
    fun record(id: String, position: Long, completed: Boolean): JSONObject {
        val value = JSONObject().put("positionInMilliseconds", position.coerceAtLeast(0))
            .put("completed", completed).put("recordedAtTimestamp", Instant.now().toString()).put("pending", true)
        synchronized(lock) { accept(id, value) }
        return value
    }
    private fun accept(id: String, value: JSONObject) { prefs.edit { putString(id,value.toString()) } }

    suspend fun resume(id: String, api: Api, signedIn: Boolean): Long {
        val remote = if (signedIn) try {
            api.playbackPosition(id)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        } else null
        // A service checkpoint may have been recorded while the remote read was in flight.
        return synchronized(lock) {
            val local = read(id)
            val best = newerProgress(local, remote)
            if (best != null && best !== local) accept(id, best)
            resumePosition(best)
        }
    }

    companion object {
        private val lock = Any()
        internal fun newerProgress(local: JSONObject?, remote: JSONObject?): JSONObject? {
            fun time(value: JSONObject?) = runCatching {
                Instant.parse(value?.optString("recordedAtTimestamp"))
            }.getOrDefault(Instant.EPOCH)
            return if (remote != null && time(remote) > time(local)) remote else local
        }
        internal fun resumePosition(value: JSONObject?): Long =
            if (value?.optBoolean("completed") == true) 0 else value?.optLong("positionInMilliseconds")?.coerceAtLeast(0) ?: 0
    }
    suspend fun sync(id: String, value: JSONObject, api: Api) {
        val timestamp = value.getString("recordedAtTimestamp")
        api.savePosition(id,value.getLong("positionInMilliseconds"),value.optBoolean("completed"),timestamp)
        synchronized(lock) {
            val latest = read(id)
            if (latest?.optString("recordedAtTimestamp") == timestamp) accept(id, latest.put("pending", false))
        }
    }
}
