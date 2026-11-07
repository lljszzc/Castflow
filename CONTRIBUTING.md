# Contributing

Use focused changes with a concrete before/after description. Follow AGENTS.md. Run the Gradle unit tests, lint and appropriate APK assembly. Python probe tests can run with `python -m unittest test_probe_account test_probe_extended`; they do not need an account. Never put live credentials in tests. State which device interactions were actually exercised. Keep source and resources buildable from a fresh clone using the wrapper.

Release source tags use vMAJOR.MINOR.PATCH. Update versionName, increase versionCode, update CHANGELOG.md and record checks. Production signing and Play distribution are not configured; do not commit a keystore to solve that.
