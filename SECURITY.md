# Security and privacy

Never post Apple passwords, cookies, media-user-token values, authorization headers, raw account responses, device backups or authenticated screenshots in issues or pull requests. Sign in only on Apple's official HTTPS pages. Credentials remain in WebView CookieManager; no JavaScript bridge or developer backend receives them. Application backup is disabled. This does not claim a custom Keystore credential vault or immunity from compromise on a rooted device.

Public source includes code and sanitized documentation only. Local captures and tools are ignored. Builds do not contain installed-account data. A fresh clone has no signed-in Apple session. Debug/performance signing keys are local and excluded; releases built on other machines use other keys.

Run `python tools/scan_secrets.py --staged` before committing. This is a guardrail, not proof against every possible secret. Review diffs and image contents manually. To report a security issue, use the repository's private vulnerability reporting feature if available rather than posting credentials publicly.
