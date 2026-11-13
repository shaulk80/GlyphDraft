# GlyphDraft: plan

Milestones with a goal and a Definition of Done (DoD). Task-level detail is written when a milestone starts, not before. Order follows the brief (docs/brief.md).

## M0 - Walking skeleton (fixed)

Goal: the whole path from commit to release works on all three platforms while the code is one screen.

DoD:
- JDK 21, Android SDK and the Gradle wrapper are set up; module split from CLAUDE.md exists with a single "Hello" Compose screen shared by desktop and Android.
- `scripts/build.ps1` passes on Windows, Linux and Android in CI: license check, build with warnings as errors, tests, package, checksums, smoke test of the extracted package (desktop under Xvfb on Linux; APK installs and launches on the emulator).
- The app writes a `*.log` file to `<data>/logs/` at start (`GLYPHDRAFT_DATA_DIR` honoured); the desktop smoke test checks for it.
- Android `applicationId` chosen (permanent once published; `scripts/smoke-android.sh` defaults to `com.glyphdraft.app`).
- Gradle task names in `scripts/build.ps1` (`checkLicenses`, `jvmTest`, `:desktopApp:createDistributable`, `testDebugUnitTest`, `:androidApp:assembleRelease`) and the Linux package list in CLAUDE.md are corrected to what actually worked.
- A pull request from branch `m0` merged through the protected `main` (rulesets applied after the first green CI run).
- `v0.0.1` pre-release pushed through `release.yml`: first a dry run (`gh workflow run release.yml --ref main`), then the tag, then a draft with 3 packages and one `SHA256SUMS.txt`, verified after download.
- Android release signing approach decided and recorded in `docs/decisions/`.
- `stacks/kotlin-multiplatform.md` suggested to the owner for the skill (what worked).

## M1 - Clickable skeleton

Goal: the owner reviews the UX before features exist.

DoD: screens for pick image, language (auto-detect or chosen), result text, copy/save, settings, on desktop and Android, driven by a stub engine returning fixed text. The owner has clicked through both and listed changes; agreed changes are done or in issues.

## M2 - OCR with language detection (first real release, v0.1.0)

Goal: give an image, get the text; language optional (auto-detected when omitted).

DoD: `OcrEngine` and `LanguageDetector` implemented on-device (candidate engines compared in a short decision record, including Cyrillic and CJK support); models download on first use with progress and a checksum; works offline afterwards; tests on a small fixture set of printed-text images in at least 3 languages and 2 scripts; v0.1.0 released.

## M3 - Translation

Goal: image + target language in, translated text out.

DoD: `Translator` implemented on-device (candidate models compared in a decision record, license checked); source language comes from M2 detection; language-pair availability visible in the UI; tested on the same fixtures.

## M4 - Handwriting

Goal: handwritten text recognised.

DoD: a handwriting-capable `OcrEngine` behind the same interface (evaluate on-device models first; if quality is poor, record that and add the optional cloud engine as the first configurable backend); a handwriting fixture set with an accuracy figure reported (warning, not gate).

## M5 - Audio

Goal: audio file in, text (optionally translated) out.

DoD: `SpeechRecognizer` interface and on-device implementation (e.g. Whisper family, size/speed trade-off recorded); supported audio formats listed; long files processed without freezing the UI, cancellable.

## M6 - Android camera

Goal: point the camera at text, get the text.

DoD: CameraX preview with permission flow; capture-and-recognise first, live recognition if performance allows; runs on a real device; the owner has tried it.

## Later (unscheduled, only if wanted)

- Engine settings screen and cloud engines (the interface is already in place).
- Play Store listing, Windows code signing or installer, Linux packages (deb, AppImage). Each is an optional gate; v1 does not depend on them.
