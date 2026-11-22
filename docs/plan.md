# GlyphDraft: plan

Milestones with a goal and a Definition of Done (DoD). The current and next milestone are in detail; closed ones are one line each, and their full text (task lists, DoD) is in `docs/history/` (`plan-M0-M1.md`, `plan-M2.md`), which is not read at session start. Task-level detail is written when a milestone starts, not before; each task is one pull request. Order follows the brief (docs/brief.md). Keep this file under 8 KB.

## Closed milestones

- **M0 - Walking skeleton.** Build, test, package and smoke-test on Windows, Linux and Android in CI; release path proven with the `v0.0.1` pre-release; Android signing decided (decisions/0002). Done 2026-11-13 (PR #1).
- **M1 - Clickable skeleton.** Pick image, language, result, copy/save and settings on desktop and Android with a stub engine; owner clicked through both (docs/manual/m1.md). Done 2026-11-14 (PR #4).
- **M2 - OCR with language detection.** On-device engine (PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract), language detection, model download on first use, offline afterwards, settings, icons, signed Android APK; owner hand test (docs/manual/m2.md, 14 findings fixed or filed). `v0.1.0` published 2026-11-22 as a pre-release.
- Open from M0: `stacks/kotlin-multiplatform.md` for the newproject skill is the owner's call.

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
