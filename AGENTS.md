# Castflow agent handoff

Read README.md, docs/ARCHITECTURE.md, docs/API.md, docs/DESIGN.md and SECURITY.md before changing behavior.

- Build the actual app in android-client. verification/android is an old login spike.
- Use JDK 17 and the checked-in Gradle 8.13 wrapper. Run testDebugUnitTest, lintDebug and the affected assemble task. Do not claim device validation from a successful build.
- Keep credentials in the official login session. Never print, commit, screenshot or export cookies, media-user-token, passwords, bearer values or private account payloads. No secrets in fixtures or CI.
- Never commit research/, reference/, .tooling/, outputs/, local.properties, keystores or runtime databases. Inspect the exact staged file list and run tools/scan_secrets.py --staged before pushing.
- Preserve the applicationId dev.podcast.nativeapp for installed users. Version 1.0.0 / code 10000 is the first public source release.
- UI state keys are currently stable Chinese identifiers; translate at presentation boundaries, not by renaming persisted keys. Update values, values-zh and values-b+zh+Hant resources together. Podcast titles/authors retain their original language. Cache keys include language and an account digest.
- Playback controls use MediaController events, not the position poll, for play/pause/buffering. System media cards must open the full player.
- Keep motion restrained and non-bouncy for full-player transitions. Retain physical screen-corner adaptation, navigation insets and keyboard safety. Do not treat project timings as official Xiaomi constants.
- Keep icon source artwork and adaptive resources in sync. Do not redistribute assets from third-party reference apps. Existing references were studied, not vendored.
- Write tests for meaningful data/state changes. For gestures, report actual device evidence and limitations; static screenshots do not prove frame-by-frame smoothness.
- API write probes require an explicitly authorized test account and the appropriate command flag. Do not run them against contributors' accounts automatically.
- Future work: extract MainActivity state into a ViewModel/repository, improve progress conflict handling/auth refresh, add content fixtures, test wider navigation/accessibility modes, and measure long-running performance.
