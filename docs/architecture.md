# GlyphDraft: architecture

Short on purpose. It says what is decided and what is already in code (M0), and lists what is open. Update it in the same pull request as any change to modules, boundaries or data locations. Reasons behind the choices are in `docs/decisions/`.

## Modules

```mermaid
graph TD
  desktopApp --> ui
  androidApp --> ui
  ui --> core
  engine --> core
  desktopApp -.-> engine
  androidApp -.-> engine
```

| Module | Targets | Holds | State |
|---|---|---|---|
| `core` | jvm, android | `Outcome` result type; `OcrEngine` and domain types (`Language`, `ImageInput`, `RecognitionResult`), the `ImagePicker` and `TextSaver` seams, `ResultBridge`, the stub engine | in code (first drafts, M2 may reshape the engine types) |
| `ui` | jvm, android | Compose Multiplatform screens (Home, Result, Settings) and `AppController` (state holder, navigation, actions), shared by both apps | in code |
| `desktopApp` | jvm | entry point, data folder, file log, FileKit file dialogs, jpackage app image for Windows and Linux | in code |
| `androidApp` | android | `MainActivity`, `MainViewModel` (composition root), system pickers, manifest, signing | in code |
| `engine` | jvm, android | on-device implementations of the engine interfaces (ONNX Runtime) | planned, created in M2 |

Rules: `core` has no UI or platform API. `ui` and `engine` depend only on `core`, never on each other. The two app modules are the composition roots: they create the engines and pass them to the UI (dotted arrows). Nothing else constructs an engine.

## Engine boundary (the part that must stay swappable)

The engine interfaces live in `core`, one per capability: text from an image (`OcrEngine`), language detection (`LanguageDetector`), translation (`Translator`), speech to text (`SpeechRecognizer`). Names and signatures are not final; M2 fixes the first two. Rules for all of them:

- Calls are suspending and return `Outcome<T>`: failure is a value with a message, not an exception. Long work is cancellable.
- Inputs are plain data (image bytes or a platform-neutral image type, an optional language hint), never a platform class.
- The composition root picks the implementation from settings. On-device is the default; a cloud engine is a later second implementation of the same interface, chosen the same way (decision 0001).

## Data flow of the first feature (M2)

```
UI: pick image (+ optional language)
  -> view model
  -> OcrEngine.recognize(image, languageHint?)   -> text
  -> LanguageDetector.detect(text)                 (only when no hint was given)
  -> UI: show text and detected language, copy / save
```
M3 adds `Translator.translate(text, from, to)` after this chain; M4 adds a handwriting-capable `OcrEngine`; M5 adds `SpeechRecognizer` as a second input; M6 adds a camera as a third source of images.

## Platform-specific code

Shared by default. Platform code is limited to what the OS dictates: picking a file or image, camera and permissions (M6), the data folder and log file, and packaging. It sits in the app modules behind small interfaces in `core` (`ImagePicker`, `TextSaver`); decided in M1: interfaces, not `expect`/`actual`, for anything the apps own. `expect`/`actual` is used only for small UI-internal details in `ui` (clipboard entry, desktop scrollbar).

On Android the system pickers answer through callbacks of the activity, which is destroyed on rotation. `ResultBridge` (in `core`) turns such a callback into a suspending call that outlives the activity; `MainViewModel` owns the bridges and the `AppController`, so state and a pending picker survive rotation. Desktop dialogs are suspending (FileKit) and need no bridge.

## Data, logs and models

- **Desktop (in code):** data folder is `GLYPHDRAFT_DATA_DIR` if set (tests and smoke tests use it), else `%APPDATA%\GlyphDraft` on Windows, `$XDG_DATA_HOME/glyphdraft` or `~/.local/share/glyphdraft` on Linux. Log: `<data>/logs/glyphdraft.log`.
- **Last folder (desktop file dialogs):** `<data>/last-folder.txt` holds one line, the folder of the last chosen file; the dialogs start there. Not read when the folder no longer exists.
- **Android:** only logcat so far; the app-private data folder and a log file are decided with the first feature that needs them. Images and saved text go through the system pickers (no storage permission); the app keeps no copy.
- **Models (planned, M2):** downloaded on first use into the data folder, with progress and a SHA-256 check; never committed or bundled in the repo. Offline after the first download.

## Build and release (summary)

One script, `scripts/build.ps1`, is run by CI and locally: license check, build with warnings as errors, tests, package, checksums, smoke test. Windows zip and Linux tar.gz come from the jpackage app image (built on their own OS); the Android APK from `assembleRelease`. Details: `CLAUDE.md`, `.github/workflows/`.

## Open, to be decided

- M2: the concrete OCR and language-detection engines (Cyrillic and CJK support is a requirement), model format and download design, image type that crosses the engine boundary, the settings model.
- M4, M5: whether handwriting and speech need a larger engine than the first on-device one, and whether that is when the cloud engine arrives.
