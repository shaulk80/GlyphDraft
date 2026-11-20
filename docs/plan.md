# GlyphDraft: plan

Milestones with a goal and a Definition of Done (DoD). The current and next milestone are in detail; closed ones are one line each, and their full text (task lists, DoD) is in `docs/history/plan-M0-M1.md`, which is not read at session start. Task-level detail is written when a milestone starts, not before; each task is one pull request. Order follows the brief (docs/brief.md). Keep this file under 8 KB.

## Closed milestones

- **M0 - Walking skeleton.** Build, test, package and smoke-test on Windows, Linux and Android in CI; release path proven with the `v0.0.1` pre-release; Android signing decided (decisions/0002). Done 2026-11-13 (PR #1).
- **M1 - Clickable skeleton.** Pick image, language, result, copy/save and settings on desktop and Android with a stub engine; owner clicked through both (docs/manual/m1.md). Done 2026-11-14 (PR #4).
- Open from M0: `stacks/kotlin-multiplatform.md` for the newproject skill is the owner's call.

## M2 - OCR with language detection (first real release, v0.1.0)

Goal: give an image, get the text; language optional (auto-detected when omitted).

DoD: `OcrEngine` and `LanguageDetector` implemented on-device (candidate engines compared in a short decision record, including Cyrillic and CJK support); models download on first use with progress and a checksum; works offline afterwards; tests on a small fixture set of printed-text images in at least 3 languages and 2 scripts; product basics for the first real release checked and done: app icon (desktop and Android), name and description as the installer shows them, privacy statement in the README (what is downloaded on first use, that images stay on the device), license, version shown in the app; v0.1.0 released.

Direction (decisions/0005, accepted): PP-OCRv5 models on ONNX Runtime in the new `engine` module; Tesseract through JavaCPP for Hebrew (no PaddleOCR model reads it); language detection by script first, then Lingua restricted to our languages. One branch per task; one task = one pull request of about 800 changed lines or fewer; status.md updated in each.

Tasks:
1. **Spike and fixtures** (`docs/spikes/m2-ocr-engines.md`). Fixture set under `core/src/commonTest/resources` or `engine/src/test/resources`: printed-text images, at least en, ru, de plus zh or ja (Latin, Cyrillic, Han at minimum), one Hebrew, a few photos with skew or noise; each with the expected text, small files with a known license (own screenshots or generated). Run the shortlist of 0005 on the JVM, record accuracy, time, model sizes, Hebrew result, where ONNX Runtime unpacks its native library, and the no-language-given strategy. **Done 2026-11-20:** report `docs/spikes/m2-ocr-engines.md`, 11 fixtures in `engine/src/test/resources/fixtures`, 0005 updated and accepted. Gate for all that follows.
2. **Engine types in `core`.** Reshape `OcrEngine` and `Language`; add `LanguageDetector`, `ModelCatalog` (which languages are usable, which models are installed), `ModelInstaller` (install with progress, cancellable). Decide what crosses the boundary (encoded bytes as now, or a decoded bitmap type). Stub implementations and tests. Keep M1 screens working. **Done 2026-11-20:** decisions/0006, image stays encoded bytes, `OcrEngine` signature unchanged.
3. **`engine` module and ONNX Runtime.** Split in two pull requests. **3a (done 2026-11-20, PR #11):** new KMP module (jvm + android), license review, a self-test with a hand-made tiny model run at app start on both targets and checked by the smoke tests, native libraries into `<data>/native` (setting `java.io.tmpdir` from `main` does not work on JDK 21), `-XX:-UsePerfData`, jar trimmed to the host OS, ABI filter, Linux CI library list. **3b (this one):** JavaCPP Tesseract and Leptonica load on both targets (`TesseractSelfTest`, no language data needed), checked by both smoke tests, and `org.bytedeco.javacpp.cachedir` into `<data>/native/javacpp`. No OCR yet.
4. **Model store and download (done 2026-11-20, decision 0007).** Manifest with URL and SHA-256 per model in the repo; download into `<data>/models`, progress, resume or restart, atomic rename after the checksum, refusal on mismatch, offline afterwards. Android data folder and `INTERNET` permission (first network use of the app). Tests with a local HTTP server and a corrupted file.
5. **Text detection and recognition (done 2026-11-20, decision 0008).** The ONNX pipeline: decode, resize, text-box detection, oriented crop, recognition, reading order. One pull request; fixtures read at 0.0 % CER on the JVM (opt-in test). Android decoder in task 8.
6. **Language detection and recognizer choice (done 2026-11-20, decision 0009).** Own two-step `LanguageDetector` (script, then common words for the Latin languages; Lingua is an 80 MB jar for four languages), `OnDeviceOcrEngine` with the "no language given" strategy (Hebrew probe seam, then the two longest lines per recognizer). Opt-in test with real models: right language and 0.0 % CER on all fixtures, with and without a language.
7. **Hebrew via Tesseract (needed: no PaddleOCR Hebrew model).** Second `OcrEngine` on the JavaCPP presets (one wrapper for JVM and Android), `heb` from `tessdata_fast`, 0.0 % CER in the spike.
8. **Wire into the apps.** Composition roots build the engine; first-use download dialog with progress, retry and cancel; error messages for missing model, no network, unreadable image; language picker from `ModelCatalog`; settings persisted in the data folder (default language, theme). Fixture tests run as integration tests on Windows, Linux and the Android host and emulator in CI.
9. **Release basics.** App icon (desktop and Android), installer name and description, README with the privacy statement and license, version shown in the app, Android release keystore and CI secrets (decisions/0002), architecture.md and CLAUDE.md updated for `engine` and the model folder.
10. **Hand test and v0.1.0.** `docs/manual/m2.md` checklist run by the owner on desktop (first-use download, offline afterwards, each fixture language, photo, wrong-language hint) and the Android emulator; fixes; `/release 0.1.0`.

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
