# GlyphDraft

AI-powered media translator: image (later handwriting, audio, camera) in, text out, optionally translated. Kotlin Multiplatform app for Windows, Linux and Android. Name cleared 2026-11-13 (docs/decisions/0001-scope-and-name.md).

Your global rules in `~/.claude` (language, code quality, tests, commits, reviews) apply. This file only adds what is specific to this repository.

## Layout

```
core/        KMP library (jvm + android): Outcome result wrapper; later the engine interfaces (OcrEngine, LanguageDetector, ...). No UI, no platform code.
ui/          KMP library (jvm + android): Compose Multiplatform screens (App.kt).
desktopApp/  JVM application (Windows, Linux): main, data folder, file log, jpackage app image.
androidApp/  Android application: MainActivity, manifest, signing.
engine/      (from M2) on-device implementations of the interfaces (ONNX Runtime); a cloud engine is a later sibling.
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
- Commits: Conventional Commits, `type(scope): summary`, one logical change per commit. Commit per task, not per milestone.
- Tests run on every target OS in CI. Coverage is a warning, never a gate.
- Do not write files outside the data folder. Its location: `GLYPHDRAFT_DATA_DIR` if set (smoke tests use this), else the per-OS app data folder. Logs go to `<data>/logs/`.

## Git and CI

- `main` is protected: work on a branch, open a pull request, merge when all checks are green. Claude commits, pushes and opens the PR when told; the owner says when to merge.
- CI (`.github/workflows/ci.yml`): jobs `build-windows` (windows-2022), `build-linux` (ubuntu-24.04, Xvfb smoke test), `build-android` (ubuntu-24.04, emulator smoke test). Each runs `scripts/build.ps1`. Packages are uploaded from `main` and from `v*` tags.
- Release: `/release <version>`. The owner pushes the `v*` tag and publishes the draft; Claude never does. Releases are unsigned (see decision 0001).

## Environment

- Windows 10 Pro. Default shell PowerShell 5.1; Git Bash available. `pwsh` (7) is NOT installed locally but is on CI runners: keep scripts compatible with both.
- Installed: git 2.33, gh 2.103, node 26, python 3.12, JDK 21 (Temurin, `JAVA_HOME` set), Android Studio Rabbit 2 with SDK platform 37 / build-tools 36 at `C:\Users\Admin\AppData\Local\Android\Sdk`, AVD `Medium_Phone_API_37.0`. No system-wide Gradle (use `gradlew`). `local.properties` (git-ignored) holds `sdk.dir` with forward slashes. Linux builds are verified by CI only. WSL not checked.
- Start the emulator headless: `emulator -avd Medium_Phone_API_37.0 -no-window -no-audio`; `adb` is in `platform-tools` (not on PATH).
- Do not install software; give the user the install steps.

## Gotchas

- Paths: do not hardcode `\` or `/`; directory enumeration order is not guaranteed; file names are case-sensitive on Linux; a zip made on Windows loses the executable bit (Linux packages are tar.gz built on Linux).
- Pin runner images (`ubuntu-24.04`, `windows-2022`), not `-latest`.
- The Linux job installs: `xvfb libgl1 libx11-6 libxext6 libxrender1 libxtst6 libxi6 libfontconfig1 fonts-dejavu-core libfreetype6 binutils` (jpackage needs `objcopy` from binutils). Untested until the first CI run; correct this list by trial.
- The JVM and Skiko (Compose's renderer) may write to the temp or cache folder (e.g. `hsperfdata`, extracted native library); check in M0 and list it here, since the app promises to write only to its data folder.
- Android job needs KVM for the emulator; the workflow enables it with a udev rule.
- The license plugin is applied per module: applied once at the root it fails with "resolution ... without an exclusive lock" under Gradle 9. New licenses go into `config/allowed-licenses.json` only after review.
- `local.properties` needs forward slashes in `sdk.dir`; backslashes are read as escapes and Gradle fails with "filename ... syntax is incorrect".
- A killed Windows process keeps its files locked for a moment; wait for it to exit before deleting (see `Stop-ProcessAndWait`).
- `gradlew` made on Windows has no executable bit in git; run `git update-index --chmod=+x gradlew` when adding it (the build script also calls it through `sh`).
- Model files are large: never commit them; they download on first use into the data folder (design in M2).

## Definition of done

- Build has no warnings; tests pass on every CI OS.
- The changed feature was run (the app, or a UI/integration test), and the summary lists what was exercised.
- PR description: what, why, test plan. `docs/status.md` updated in 2 to 5 lines.
- No TODO without an issue.

## Where to look

- `docs/status.md` - current state and next step. Start here.
- `docs/plan.md` - milestones and their Definition of Done.
- `docs/decisions/` - one short file per decision.
- GitHub Issues - bugs and backlog.
