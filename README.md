# 听见 · Castflow

**A native Android podcast player with soft glass UI, Apple Podcasts browsing and personal library support.**

听见（Castflow）是一款原生 Android 播客客户端：使用 Kotlin、Jetpack Compose 与 Media3，提供柔光玻璃界面、播客发现、搜索、排行榜、资料库和后台播放。中文系统显示「听见」，其他语言系统显示「Castflow」，界面支持中文和英文。

<img src="design/icon-concepts/castflow-a-red-3d-fullbleed.png" width="160" alt="Castflow icon" />

## Version 1.1.0

- 首页、新发现、分类浏览、节目与单集排行榜、节目及单集搜索。
- Apple 官方网页登录；个人资料库、关注节目、保存单集、最近播放和继续收听。
- Media3 后台播放与系统媒体卡片，倍速、跳转、待播队列、睡眠定时。
- 单集下载及 Wi-Fi 下载偏好、本地置顶与分类设置。
- 封面取色、柔光玻璃材质、播放页与迷你播放器连续转场。
- 页面内存/磁盘缓存、后台刷新、更新内容置前和轻量排序动画。
- 按系统语言显示中英文名称与界面。

This is an independent community project, not an Apple or Xiaomi product. Catalog and account APIs are undocumented and may change. Apple authentication is the only WebView flow; browsing and playback are native.

### Current boundaries

1.0 labels this first open-source version; it does not imply feature parity with Apple Podcasts. Paid/protected media, transcripts, chapters, full channel pages, long-lived authentication renewal, and robust cross-device progress conflict resolution are not complete. The application currently targets SDK 36 and requires Android 12 / API 31 or newer. Some validation was performed on Android 17 / HyperOS 4; this is not a claim of complete Android 17 API adoption or stable 120 Hz performance.

## Build

To continue development on another machine, clone the repository and read
`AGENTS.md`. The `main` branch contains ongoing work. To start a branch from
the exact 1.1 release, run:

```bash
git clone https://github.com/lljszzc/Castflow.git
cd Castflow
git switch -c work-from-1.1 v1.1.0
```

Install JDK 17 and Android SDK platform 36 / build-tools 35.0.0. Set `JAVA_HOME` and `ANDROID_HOME` (or create an untracked `android-client/local.properties` containing `sdk.dir=...`).

```bash
cd android-client
./gradlew testDebugUnitTest lintDebug assembleDebug
# Windows: .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

`assemblePerformance` creates an optimized, non-debuggable development APK signed with the local Android debug key. A clean machine uses a different debug key; it cannot update an APK signed by another key. Signing keys are never included in this repository. No production signing infrastructure is configured.

For Android 17 cold-start and anomaly profiling, build `assembleProfileLab`.
This creates a debuggable installation with the separate package ID
`dev.podcast.nativeapp.profilelab`. Use it signed out with synthetic or public content.
Launch once to register the triggers, then cold-start the lab app again. Use
`adb logcat -s CastflowProfiles:I` for registration and result status and
`adb shell run-as dev.podcast.nativeapp.profilelab ls files/profiling` to inspect local artifacts.
Android controls sampling, rate limits and delivery. Debuggable lab timings can differ
from the regular performance APK. Uninstall the lab package to clear its diagnostics.

## Documentation

- [Architecture and navigation](docs/ARCHITECTURE.md)
- [API contracts and verification](docs/API.md)
- [UI, motion and localization](docs/DESIGN.md)
- [Security and publication boundaries](SECURITY.md)
- [Agent maintenance instructions](AGENTS.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)

`verification/android` is a historical, source-only authentication spike, not the released application. Python probes are development tools; see [verification notes](docs/VERIFICATION.md) before running write tests. Captures, credentials, account responses, screenshots, SDKs, build outputs and signing keys are deliberately excluded.

## License

MIT for project-authored code and artwork. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md). Apple Podcasts, Apple Music, Xiaomi and HyperOS names belong to their respective owners. External podcast artwork and media belong to their publishers and are fetched at runtime, not distributed as project assets.
