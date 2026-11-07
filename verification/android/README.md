# Android 登录与原生接口验证工具

这是认证技术验证工程，不是完整播放器。主页面中的 WebView 仅供 Apple 官方登录，顶部“验证”按钮使用 Java 的原生 HTTPS 客户端直接读取账户接口。

验证事项：

1. 小米 17 / Android 17 / HyperOS 4 能否完成 Apple 登录与双重认证。
2. 点击验证，账户地区、节目、已保存、最近播放和继续播放是否全部返回 200。
3. 关闭并重新打开工具，再点击验证，确认 WebView Cookie 会话是否保留。

凭据只由应用内 WebView CookieManager 保留；原生请求读取当前会话值后仅放在请求头中，不显示、不打印。禁用应用备份，没有 JavaScript bridge，不导出 Cookie 或账户响应正文。认证窗口允许 HTTPS Apple 域名；原生账户请求仅允许两个固定主机，禁止自动跟随重定向。

该实验使用 Android WebView 自身 Cookie 存储，尚未实现正式应用的 Keystore 凭据仓库。SDK target 35，用于先验证 Android 17 向后兼容行为；正式应用 target SDK 将按发布环境调整。验证工具不会写资料库、购买订阅或提交播放事件。

构建：从项目根目录执行 `.tooling/gradle-8.13/bin/gradle.bat -p verification/android assembleDebug`，并设置 JAVA_HOME 为本地 .tooling 下 JDK、ANDROID_HOME 为 .tooling/android-sdk。

安装包：`app/build/outputs/apk/debug/app-debug.apk`。

未验证：长时间令牌到期和自动刷新、退出清理、后台音频、下载、付费内容。网页登录成功不自动证明这些功能可用。
