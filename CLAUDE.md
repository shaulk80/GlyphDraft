# GlyphDraft

AI-powered media translator: image (later handwriting, audio, camera) in, text out, optionally translated. Kotlin Multiplatform app for Windows, Linux and Android. Name cleared 2026-11-13 (docs/decisions/0001-scope-and-name.md).

Your global rules in `~/.claude` (language, code quality, tests, commits, reviews) apply. This file only adds what is specific to this repository.

## Layout

```
core/        KMP library (jvm + android): Outcome result wrapper; later the engine interfaces (OcrEngine, LanguageDetector, ...). No UI, no platform code.
ui/          KMP library (jvm + android): Compose Multiplatform screens (App.kt).
desktopApp/  JVM application (Windows, Linux): main, data folder, file log, jpackage app image.
androidApp/  Android application: MainActivity, manifest, signing.
engine/      KMP library (jvm + android): on-device implementations of the interfaces (ONNX Runtime; so far only the self-test and the desktop native-library loader); a cloud engine is a later sibling.
config/      allowed-licenses.json (license allow-list for `checkLicense`)
scripts/     build.ps1 (the one build script for CI and local) + Common/Checksums/Package/Smoke.ps1, smoke-android.sh
docs/        status.md, plan.md, brief.md, decisions/
```

## Commands

```
.\gradlew jvmTest :desktopApp:test       # desktop-side tests (core, ui, desktopApp)
.\gradlew testAndroidHostTest            # the same shared code tested as Android host tests
.\gradlew :core:jvmTest --tests "*OutcomeTest*"   # one test class
.\gradlew :desktopApp:run                # run the desktop app
.\gradlew checkLicense                   # dependency license check (all modules)
.\scripts\build.ps1                      # CI-equivalent: licenses, build, test, package, checksums, smoke (host OS)
.\scripts\build.ps1 -Target android      # APK; then bash scripts/smoke-android.sh with an emulator running
```

Version lives only in `gradle.properties` (`appVersion`). Packages land in `dist/`.

## Rules

- Language: Kotlin (K2), Gradle Kotlin DSL, version catalog in `gradle/libs.versions.toml`. Dependencies direction: `ui`, `engine` -> `core`; apps -> all. `core` never references a UI or platform API.
- Engines sit behind interfaces in `core`; the app picks an engine in the composition root from settings. Never call an engine class from the UI directly.
- Errors are `Result` values (`core`), not exceptions. Coroutines for all I/O; no blocking calls on the main thread.
- Commit messages and PR descriptions: no `Co-Authored-By` and no "Generated with Claude Code" line (owner rule; overrides defaults). `.claude/settings.json` sets `attribution` to empty so Claude Code does not add them.
- Commits: Conventional Commits, `type(scope): summary`, one logical change per commit. Commit per task, not per milestone.
- Tests run on every target OS in CI. Coverage is a warning, never a gate. A flaky check gets one retry in its script (logged) and an issue; fix it before the next milestone.
- One task per pull request, about 800 changed lines or fewer (tests and generated files not counted).
- A problem that passes about 60 tool calls with no result: stop, write what was learned to `docs/spikes/<name>.md`, and recommend that the user start a fresh session.
- Do not write files outside the data folder. Its location: `GLYPHDRAFT_DATA_DIR` if set (smoke tests use this), else the per-OS app data folder. Logs go to `<data>/logs/`, ONNX Runtime natives to `<data>/native/`. Known exceptions: ONNX Runtime makes an empty `onnxruntime-java*` folder in the system temp folder (it removes it on normal exit; a forced kill leaves it); Skiko is still to be checked.

## Git and CI

- `main` is protected: work on a branch, open a pull request, merge when all checks are green. Claude commits, pushes and opens the PR when told; the owner says when to merge.
- CI (`.github/workflows/ci.yml`): jobs `build-windows` (windows-2022), `build-linux` (ubuntu-24.04, Xvfb smoke test), `build-android` (ubuntu-24.04, emulator smoke test). Each runs `scripts/build.ps1`. Packages are uploaded from `main` and from `v*` tags.
- Release: `/release <version>`. The owner pushes the `v*` tag and publishes the draft; Claude never does. Releases are unsigned (see decision 0001).

## Environment

- Windows 10 Pro. Default shell PowerShell 5.1; Git Bash available. `pwsh` (7) is NOT installed locally but is on CI runners: keep scripts compatible with both.
- Installed: git 2.33, gh 2.103, node 26, python 3.12, JDK 21 (Temurin, `JAVA_HOME` set), Android Studio Rabbit 2 with SDK platform 37 / build-tools 36 at `C:\Users\Admin\AppData\Local\Android\Sdk`, AVD `Medium_Phone_API_37.0`. No system-wide Gradle (use `gradlew`). `local.properties` (git-ignored) holds `sdk.dir` with forward slashes. Linux builds are verified by CI only. WSL not checked.
- Start the emulator headless: `emulator -avd Medium_Phone_API_37.0 -no-window -no-audio`; `adb` is in `platform-tools` (not on PATH).
- After an install, refresh PATH in the session with `. .\scripts\env.ps1`.
- Do not install software; give the user the install steps.

## Gotchas

- Edit files with the Edit and Write tools, never with shell heredocs (quoting breaks on Windows). Run Gradle and PowerShell scripts in PowerShell, not Git Bash.
- Never delete with a glob after `cd` (the safety check blocks it); name a new empty folder instead.
- Paths: do not hardcode `\` or `/`; directory enumeration order is not guaranteed; file names are case-sensitive on Linux; a zip made on Windows loses the executable bit (Linux packages are tar.gz built on Linux).
- Pin runner images (`ubuntu-24.04`, `windows-2022`), not `-latest`.
- The Linux job installs: `xvfb libgl1 libx11-6 libxext6 libxrender1 libxtst6 libxi6 libfontconfig1 fonts-dejavu-core libfreetype6 binutils` (jpackage needs `objcopy` from binutils). Verified by a green run on 2026-11-13 (PR #1); add a library here when a new one is needed.
- `java.io.tmpdir` cannot be changed from `main` (JDK 17+ reads it once at startup); only a `-D` JVM option works, and a packaged app does not know its data folder. So ONNX Runtime is pointed at `<data>/native/` with `onnxruntime.native.path` (`OnnxNativeLibraries.prepare`, called before first use). The packaged JVM runs with `-XX:-UsePerfData` so that no `hsperfdata` file is written to the temp folder.
- `desktopApp` swaps the ONNX Runtime jar for a copy trimmed to the build host's OS (`trimOnnxruntime`; 55 MB to 7 MB); the Android APK is limited to `arm64-v8a` and `x86_64`. An engine library that is added later must be checked for both.
- Maven Central answers `429 Too Many Requests` to CI runners now and then (seen twice on `build-android` on 2026-11-20). `Invoke-Gradle` repeats a run once, with a `RETRY:` line in the log, when its output shows a refused or timed-out download (pattern in `scripts/Common.ps1`); any other failure is final at once. A `RETRY:` line in a green run means the check is still flaky.
- In PowerShell functions, native program output becomes part of the return value; pipe it to `Out-Host` (see `Invoke-Gradle`), otherwise `-not (f)` is never true and a failed Gradle run goes unnoticed.
- The JVM and Skiko (Compose's renderer) may write to the temp or cache folder (e.g. `hsperfdata`, extracted native library); check in M0 and list it here, since the app promises to write only to its data folder.
- The license plugin is applied per module: applied once at the root it fails with "resolution ... without an exclusive lock" under Gradle 9. New licenses go into `config/allowed-licenses.json` only after review.
- `local.properties` needs forward slashes in `sdk.dir`; backslashes are read as escapes and Gradle fails with "filename ... syntax is incorrect".
- `gradlew` made on Windows has no executable bit in git; run `git update-index --chmod=+x gradlew` when adding it (the build script also calls it through `sh`).
- JavaCPP (Tesseract, Leptonica) unpacks about 20 MB of natives on the desktop; `JavaCppNativeLibraries.prepare` sets `org.bytedeco.javacpp.cachedir` to `<data>/native/javacpp` before first use. The native jars are classifier jars chosen by the app (`desktopApp`: the build host's platform from `javacppHostPlatform` in the root build file; `androidApp`: `android-arm64` and `android-x86_64`). On Android the same jars also carry C headers, CMake files and a `tesseract` executable under `lib/<abi>/`, and the installer then fails with "Failed to extract native libraries"; `androidApp` excludes them in `packaging.resources` (also the duplicate `META-INF/native-image`). The two JavaCPP libraries add about 34 MB to the APK.
- Model files are large: never commit them; they download on first use into `<data>/models` (`engine/.../models`, decision 0007). `ModelManifest` pins each file by commit-pinned URL, SHA-256 and size; to change a model, change all three together. `RealDownloadTest` hits the real servers and runs only with `GLYPHDRAFT_NETWORK_TESTS=1`; run it after touching the manifest. `FixtureOcrTest` reads the fixtures with the real models from `GLYPHDRAFT_MODELS_DIR` (default `engine/build/test-models`, filled when `GLYPHDRAFT_NETWORK_TESTS=1`) and does nothing without them; run it after touching the OCR pipeline.

## Definition of done

- Build has no warnings; tests pass on every CI OS.
- The changed feature was run (the app, or a UI/integration test), and the summary lists what was exercised.
- PR description: what, why, test plan. `docs/status.md` updated in 2 to 5 lines, in the same pull request (no record-only pull requests; keep it under 3 KB); `docs/architecture.md` too if modules, boundaries or data locations changed.
- Hand-test results the owner reports are written into `docs/manual/<milestone>.md` in the same session.
- No TODO without an issue.

## Where to look

- `docs/status.md` - current state and next step. Start here.
- `docs/plan.md` - the current and next milestone in detail, closed ones in one line each (under 8 KB). Full old plans: `docs/history/`.
- `docs/manual-testing.md` - how to run the hand tests and what to click; `docs/manual/` - results and checklists per milestone (step, expected, result, date).
- `docs/architecture.md` - modules, engine boundary, data flow; update it when those change.
- `docs/decisions/` - one short file per decision. GitHub Issues - bugs and backlog.
