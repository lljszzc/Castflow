# Changelog

## 1.2.0 — 2026-11-14

- 播放单集时自动建立所属节目的播放队列，系统媒体控件补齐可用的上一集和下一集。
- 增加搜索、最新单集和最近播放三个桌面快捷方式，使用彩色圆形图标背景。
- 完整播放器填充屏幕矩形边界，长标题支持慢速 marquee，封面随播放状态平滑缩放。
- 标题、保存和更多操作采用光学校正后的整体居中布局，避免长标题造成视觉重心偏移。
- 将 Palette 依赖切换为 KTX 版本，Debug Lint 保持无问题。

## 1.1.0 — 2026-11-10

- 新发现页直接展示节目、单集和订阅节目榜单，可从栏目标题进入完整榜单。
- 播放页采用连续进度轨道，支持拖动预览；预测性返回保留松手后的收起动画。
- 编辑置顶项、编辑类别和设置面板退出时向下收起，遮罩同步淡出。
- 启动窗口与首页统一为白色，搜索分类标题统一为白色，应用图标简化了背景渐变。
- 减少暂停播放和空闲下载时的定时工作，改进离线播放、进度冲突处理和页面缓存。
- 增加独立的 Android 17 本地诊断构建。日常安装包使用原应用 ID。

## 1.0.0 — 2026-11-07

First public source release of 听见 / Castflow: native browsing, search/charts/library, official Apple sign-in, Media3 playback and queue, downloads and progress synchronization, glass UI, adaptive red icon, Chinese/English presentation and per-account page caching. See README for incomplete features and verification limits.
