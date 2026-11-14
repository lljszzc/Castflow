package dev.podcast.nativeapp

internal object ShortcutActions {
    const val SEARCH = "dev.podcast.nativeapp.SEARCH"
    const val LATEST_EPISODE = "dev.podcast.nativeapp.LATEST_EPISODE"
    const val RECENTLY_PLAYED = "dev.podcast.nativeapp.RECENTLY_PLAYED"
}

internal enum class ShortcutDestination(val tab: String, val section: String) {
    SEARCH("搜索", "概览"),
    LATEST_EPISODE("资料库", "最新单集"),
    RECENTLY_PLAYED("资料库", "最近播放");

    companion object {
        fun fromAction(action: String?): ShortcutDestination? = when (action) {
            ShortcutActions.SEARCH -> SEARCH
            ShortcutActions.LATEST_EPISODE -> LATEST_EPISODE
            ShortcutActions.RECENTLY_PLAYED -> RECENTLY_PLAYED
            else -> null
        }
    }
}
