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

Planned 2026-11-23. One branch and one pull request per task (about 800 changed lines or fewer); `docs/status.md` updated in each. Release as `v0.2.0` (pre-release) after the hand test.

Starting points (all to be verified in task 1, none is a decision):
- Languages: the eight of the pickers (en, ru, he, de, fr, es, zh, ja). Translation models are one direction per file, so English is the pivot: 7 languages x 2 directions = 14 models; any other pair runs twice (X to en to Y) and the UI says so. A model download is about 40 to 110 MB per direction (int8), so a pack is one language with both directions, shown with its size.
- Candidates: (a) OPUS-MT (Marian) exported to ONNX on ONNX Runtime, which the app already ships, with a SentencePiece tokenizer and our own decode loop; (b) Bergamot / Firefox Translations models (MPL 2.0 as reported, small, English pivot) which need a native Marian build for three platforms unless run through ONNX; (c) CTranslate2, no official JVM or Android binding found; (d) multilingual M2M100, MADLAD or an LLM: one model for all pairs but large and slow. Rejected on sight: ML Kit Translate (Android only, proprietary); NLLB-200 (license believed non-commercial, confirm). (a) is the favourite because it adds no new native library.
- OCR text arrives as lines, translation works on sentences: a segmenter (paragraphs and sentences, lines of one paragraph joined) is part of the work, and the result keeps the paragraph layout.

Tasks:
1. **Spike, gate for the rest** (`docs/spikes/m3-translation.md`, decision 0012). Run the candidates on the JVM over a small reference set (about 30 sentences per direction, own text with own references, in `engine/src/test/resources/translation`): quality as chrF (BLEU as well if cheap), time per sentence, memory, model size, license of code and weights, whether it can run on Android, tokenizer options (JNI SentencePiece or own Kotlin implementation). Record which of the 14 directions are good enough; Hebrew, Japanese and Chinese are the likely weak ones. It also sets which directions are marked experimental (chrF threshold) and checks the pack size against the ceiling below. Stop and write up after about 60 tool calls (rule in `CLAUDE.md`).
2. **Types in `core`** (decision 0013). `Translator` (`suspend translate(text, from, to): Outcome<Translation>`, cancellable), `Translation` (text, from, to, pivoted); translation packs in the model catalog (a pack serves directions, not "readable languages"; decide whether `ModelPack` grows a kind or a second catalog is added), `Settings.defaultTarget`. Stubs and tests; M2 screens keep working.
3. **Segmenter and tokenizer** (`engine`, shared code). Paragraph and sentence splitting (Latin, Cyrillic, Hebrew, CJK punctuation), rejoining lines, keeping blank lines; the tokenizer chosen in task 1 behind a small interface; unit tests with parameterized cases.
4. **Translation packs in the manifest.** Pinned URL, SHA-256 and size per file (as 0007), the pack-to-direction mapping, pivot planning (`en` hub; which packs a pair needs), tests with the local model server.
5. **The translator.** Model sessions, decode loop (greedy or beam, as task 1 finds), sentence batching, cancellation, memory release on close; opt-in fixture test with the real models (chrF per direction reported as a warning, a floor per direction as a regression guard). Split in 5a (one direction end to end on the JVM) and 5b (all packs, pivot) if over the size limit.
6. **Engine wiring and Android.** `OnDeviceEngine` exposes the translator; both composition roots pass it on; the translator runs on the Android emulator (model size, memory, time); error messages for a missing pack, no network, text too long.
7. **UI flow.** `AppController` and `AppState`: a target language (null means "no translation", the default), translate after the read, or later from the Result screen without reading again; `InstallFlow` also asks for the translation packs a pair needs; cancel and Back work as for a read. Stub translator for tests; `AppControllerTest` cases.
8. **UI screens.** Target picker on Home, original and translation on Result (toggle), copy and save of either, pair availability marked in the pickers ("download needed", "via English"), translation packs in the model list and a default target in Settings, a notice when the source language is unknown or equals the target. Click-through test.
9. **End to end and docs.** Opt-in test image to translation over the fixtures (several pairs, both Latin and non-Latin sources); README (privacy: what the translation download is, text stays on the device), `architecture.md` (data flow, module table), `CLAUDE.md` gotchas, manual checklist `docs/manual/m3.md`.
10. **Hand test and v0.2.0.** The owner runs the checklist on desktop and the emulator (first-use download, offline afterwards, each pair type, pivot, a photo, cancel during translation); fixes; `/release 0.2.0`.

Decided by the owner 2026-11-23: M3 goes before the open issues (#30 parked; #8, #12, #15 are CI flakes). A weak direction ships marked "experimental" in the pickers, it is not dropped (task 8 shows the mark; the spike sets the threshold). Size ceiling: about 150 MB per language pack (both directions), a working figure; the hand test (task 10) shows whether it is right.

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
