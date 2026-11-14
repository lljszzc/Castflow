# Architecture

- MainActivity.kt: navigation, launcher shortcut intents, page loading, account state and MediaController. It is still a large composition; separate state/repositories incrementally.
- Api.kt: catalog, editorial, account requests, pagination and runtime public authorization discovery. Storefront comes from the signed-in account when available.
- PlaybackService.kt: MediaSessionService / ExoPlayer, audio focus, queue persistence, timer and progress checkpoints. Playing an episode builds a show-aware queue so system previous/next controls have real targets. SessionActivity opens the player via dev.podcast.OPEN_PLAYER.
- ProgressStore.kt: local progress/resume and serialized account synchronization. Downloads.kt: Android DownloadManager and local metadata.
- PageDiskCache.kt: display-only JSON, atomic writes, account-digest filenames, language-specific page keys, 24-page bound and seven-day expiry. No credentials or audio URLs in snapshots. The OS may evict the cache.
- BottomDock.kt / FullPlayer.kt: native glass navigation/mini-player and player gestures. The cover travels between full and mini geometry, scales with playback state, and shortens overflowing titles to a slow marquee. Corner radii come from WindowInsets.
- Materials.kt, GlassInteraction.kt, GlassMenu.kt, SoftOptions.kt, MoreMenuButton.kt, MotionTokens.kt: shared rendering and feedback.
- LibraryPreferences.kt: stable local categories/pins. UiStrings.kt / LocalizedComponents.kt: presentation-only resource adaptation; known server-provided category headings have English mappings.

Preferences, cookies, downloads and cache belong to the installed app sandbox. They are not part of a build or release artifact. The project uses no developer-controlled backend.
