# 0001: Scope, name, platforms, stack

Date: 2026-11-13. Status: accepted.

- **Name: GlyphDraft** (proposed in the brief, kept by the owner). Checked: web search, GitHub, npm, NuGet, PyPI, crates.io, pub.dev (no hits; domains glyphdraft.com/.app/.io/.net/.org unregistered per RDAP). One GitHub repo of the same name exists (Chaitanya-UXUI/GlyphDraft, a typeface design tool, 1 day of activity in April 2026). **Not checked:** USPTO, TMview, WIPO (need a browser). Do this before a public launch.
- **v1 platforms: Windows (x64), Linux (x64), Android.** All three build and are smoke-tested in CI from M0. Camera input is Android only (M6).
- **External gates and fallbacks:** none is required for v1. Windows is shipped unsigned (zip, SmartScreen warning documented). Linux is a tar.gz. Android is an APK from GitHub Releases, signed with a self-generated key (debug key in M0). Play Store, Windows code signing and an installer are optional later milestones; if declined or skipped, nothing in v1 changes. Keep the Android key stable: a changed key blocks updates.
- **Stack: Kotlin Multiplatform + Compose Multiplatform** (owner chose it from an 8-option comparison). Reasons: native Android for the camera step, one shared UI and logic, JVM desktop packaged per OS with jpackage, ONNX Runtime has a JVM build for all three OSes. Cost: desktop installers carry a JVM (50-100 MB).
- **AI runs on-device first** (owner decision), behind interfaces in `core` (OcrEngine, LanguageDetector, Translator, SpeechRecognizer) so engines are configurable later, including a cloud backend. Concrete models are chosen per milestone, not now.
- **License: Apache-2.0, public repository.** Third-party models need their licenses checked before bundling or downloading them.
- **Tool versions seen on 2026-11-13** (re-check in M0): Kotlin 2.4.x stable, Compose Multiplatform 1.12.x, AGP 9.5.x, Gradle 9.9, ONNX Runtime 1.32. CI actions: checkout v7, setup-java v6, gradle/actions v6, upload-artifact v7, download-artifact v8, android-emulator-runner v2.
