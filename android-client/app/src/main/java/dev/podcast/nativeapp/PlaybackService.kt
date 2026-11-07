package dev.podcast.nativeapp

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.webkit.CookieManager
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    companion object { var active: PlaybackService? = null; private set }
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sleep = Runnable { session?.player?.pause() }
    fun setSleep(minutes: Int) { handler.removeCallbacks(sleep); if (minutes > 0) handler.postDelayed(sleep, minutes * 60_000L) }
    private var session: MediaSession? = null
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()
    private val progress by lazy { ProgressStore(this) }
    private fun userToken() = CookieManager.getInstance().getCookie("https://podcasts.apple.com")?.split(';')?.map { it.trim() }?.firstOrNull { it.startsWith("media-user-token=") }?.substringAfter('=')
    private val api by lazy { Api(::userToken) }
    private fun saveQueue() {
        val p = session?.player ?: return
        val items = JSONArray()
        for(i in 0 until p.mediaItemCount) {
            val media = p.getMediaItemAt(i)
            val uri = media.localConfiguration?.uri ?: continue
            items.put(JSONObject().put("id",media.mediaId).put("uri",uri.toString()).put("title",media.mediaMetadata.title).put("author",media.mediaMetadata.artist).put("artwork",media.mediaMetadata.artworkUri?.toString()))
        }
        getSharedPreferences("playback-queue",MODE_PRIVATE).edit().putString("items",items.toString()).putInt("index",p.currentMediaItemIndex.coerceAtLeast(0)).putLong("position",p.currentPosition.coerceAtLeast(0)).apply()
    }
    private fun checkpoint() {
        saveQueue()
        val player = session?.player ?: return
        val id = player.currentMediaItem?.mediaId ?: return
        if(!id.matches(Regex("\\d+")) || player.currentPosition <= 0) return
        val value = progress.record(id, player.currentPosition, player.playbackState == Player.STATE_ENDED)
        if(userToken() != null) syncScope.launch { syncLock.withLock { runCatching { progress.sync(id,value,api) } } }
    }
    private val tick = object : Runnable { override fun run() { if(session?.player?.isPlaying == true) checkpoint(); handler.postDelayed(this,15000) } }
    override fun onCreate() {
        super.onCreate()
        active = this
        val player = ExoPlayer.Builder(this).build()
        player.setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        player.setHandleAudioBecomingNoisy(true)
        val activity = android.app.PendingIntent.getActivity(this, 1,
            android.content.Intent(this, MainActivity::class.java).setAction("dev.podcast.OPEN_PLAYER")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).setCallback(object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                return if(controller.packageName == packageName || controller.isTrusted) super.onConnect(session,controller) else MediaSession.ConnectionResult.reject()
            }
        }).build()
        runCatching {
            val prefs = getSharedPreferences("playback-queue",MODE_PRIVATE)
            val restored = JSONArray(prefs.getString("items","[]")).items().map { item ->
                MediaItem.Builder().setMediaId(item.getString("id")).setUri(item.getString("uri")).setMediaMetadata(MediaMetadata.Builder().setTitle(item.optString("title")).setArtist(item.optString("author")).setArtworkUri(android.net.Uri.parse(item.optString("artwork"))).build()).build()
            }
            if(restored.isNotEmpty()) { player.setMediaItems(restored,prefs.getInt("index",0).coerceIn(0,restored.lastIndex),prefs.getLong("position",0)); player.prepare() }
        }
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { saveQueue() }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if(!isPlaying) checkpoint() }
            override fun onPlaybackStateChanged(playbackState: Int) { if(playbackState == Player.STATE_ENDED) checkpoint() }
        })
        handler.postDelayed(tick,15000)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() { checkpoint(); handler.removeCallbacks(tick); syncScope.cancel(); handler.removeCallbacks(sleep); active = null; session?.run { player.release(); release() }; super.onDestroy() }
}
