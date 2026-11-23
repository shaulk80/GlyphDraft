# GlyphDraft: plan

Milestones with a goal and a Definition of Done (DoD). The current and next milestone are in detail; closed ones are one line each, and their full text (task lists, DoD) is in `docs/history/` (`plan-M0-M1.md`, `plan-M2.md`), which is not read at session start. Task-level detail is written when a milestone starts, not before; each task is one pull request. Order follows the brief (docs/brief.md). Keep this file under 8 KB.

## Closed milestones

- **M0 - Walking skeleton.** Build, test, package and smoke-test on Windows, Linux and Android in CI; release path proven with the `v0.0.1` pre-release; Android signing decided (decisions/0002). Done 2026-11-13 (PR #1).
- **M1 - Clickable skeleton.** Pick image, language, result, copy/save and settings on desktop and Android with a stub engine; owner clicked through both (docs/manual/m1.md). Done 2026-11-14 (PR #4).
- **M2 - OCR with language detection.** On-device engine (PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract), language detection, model download on first use, offline afterwards, settings, icons, signed Android APK; owner hand test (docs/manual/m2.md, 14 findings fixed or filed). `v0.1.0` published 2026-11-22 as a pre-release.
- Open from M0: `stacks/kotlin-multiplatform.md` for the newproject skill is the owner's call.

## M3 - Translation

Goal: image + target language in, translated text out.

DoD: `Translator` implemented on-device (models and license recorded in decision 0012); source language comes from M2 detection; language-pair availability visible in the UI; tested on the same fixtures.

Planned 2026-11-23, updated after the spike the same day. One branch and one pull request per task (about 800 changed lines or fewer); `docs/status.md` updated in each. Release as `v0.2.0` (pre-release) after the hand test.

Spike result (`docs/spikes/m3-translation.md`, decision 0012): OPUS-MT (Marian) int8 on the ONNX Runtime the app ships, own greedy decode loop and own Kotlin SentencePiece tokenizer, no new native library. Models are one direction each, about 110 MB. chrF on 30 own sentences: 70 to 86 for en with de, fr, es, ru; zh to en and ja to en 72; en to zh readable (experimental); he to en 82 but 366 MB; ru to he 68 (144 MB). Not usable: en to ja (8), en to he (3) and he to ru (0.4, broken exports).
- Hubs: English for everything, Russian as a second hub for Hebrew. A pair runs two or three models by a route (de to he = de, en, ru, he) and the UI says "via English" or "via English and Russian".
- Targets offered: en, de, fr, es, ru, zh, he. Sources: all eight. Japanese is never a target (no model).
- Download unit is one direction. Experimental: chrF below 60 on the reference set, or a CJK target judged by reading; every Hebrew direction until a native speaker has read it.

Tasks:
1. **Spike** (done 2026-11-23).
2. **Types in `core`** (decision 0013, done 2026-11-23). `Translator` (`suspend translate(text, from, to): Outcome<Translation>`, cancellable), `Translation` (text, from, to, route), a catalog of translation directions (a model is a direction, not a "readable language"; decide whether `ModelPack` grows a kind or a second catalog is added), `Settings.defaultTarget`. Stubs and tests; M2 screens keep working.
3. **Segmenter and tokenizer** (done 2026-11-23; `engine/.../translation`). Paragraph and sentence splitting (Latin, Cyrillic, Hebrew, CJK punctuation), rejoining OCR lines, keeping blank lines; the unigram tokenizer from the spike behind a small interface; parameterized tests.
4. **Direction manifest and routes** (done 2026-11-23, decision 0014; the Hebrew mirror is still open, owner to say where). Pinned URL at a commit, SHA-256 and size per file (as 0007); route planner over the hubs; tests with the local model server. Hebrew files come from one private uploader: mirror them (decide where) and pin the mirror.
5. **The translator** (done 2026-11-23, decision 0015). Greedy decode with and without key/value cache, one direction open at a time, cancellation, ids from the model; opt-in fixture test with chrF per direction (reference set in `engine/src/test/resources/translation`). Beam search and sentence batching were left out (see the decision).
6. **Hebrew directions** (done 2026-11-23, decision 0016). The uploader's en-he, he-ru, de-he, es-he, fr-he and he-es exports are broken (chrF 0 to 9), he-de works (71.6) but the route through English is better (75.6), so no direction was added; all Hebrew pairs are measured through the hubs (62 to 79) and kept as test cases. Native read done by the owner (`docs/manual/m3-hebrew-read.md`; experimental stays), no mirror, no own export. Decide whether he to en (366 MB) stays after task 7 shows its memory on Android.
7. **Engine wiring and Android** (done 2026-11-23, decision 0017). `OnDeviceEngine` exposes the translator and the catalog holds the translation packs; run on the emulator with a debug-only probe (memory, time, downloads); `he to en` stays optional. `AppController` takes the translator in task 8, where it is first used.
8. **UI flow.** `AppController`, `AppState`: target language (null means no translation, the default), translate after the read or later from Result; `InstallFlow` also asks for the models a route needs, with the total size; cancel and Back as for a read. Stub translator; `AppControllerTest` cases.
9. **UI screens.** Target picker on Home, original and translation on Result (toggle), copy and save of either, route availability in the pickers ("download needed", "via English"), experimental mark, translation models in the model list, default target in Settings, notice when the source is unknown or equals the target. Click-through test.
10. **End to end and docs.** Opt-in test image to translation (several routes, Latin and non-Latin sources, Hebrew); README (privacy, CC-BY credits for Helsinki-NLP, OPUS-MT and the converters, also in Settings); `architecture.md`; `CLAUDE.md` gotchas; checklist `docs/manual/m3.md`.
11. **Hand test and v0.2.0.** Owner runs the checklist on desktop and the emulator (first-use download, offline afterwards, each route type, a photo, cancel); fixes; `/release 0.2.0`.

Decided by the owner 2026-11-23: M3 before the open issues (#30 parked; #8, #12, #15 are CI flakes); weak directions ship marked "experimental"; Hebrew is included. Size: the 150 MB figure becomes about 115 MB per direction, he to en (366 MB) an optional exception (decision 0012, accepted).

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
- Online translation (idea 2026-11-23, not decided): an optional `Translator` that calls a cloud service with the user's own API key, off by default, on-device stays the fallback. Gains: no gaps (en to ja, en to he), direct pairs, better quality, no model downloads. Costs: text leaves the device (breaks the privacy promise, needs an opt-in and README changes), key storage, provider terms, no CI without secrets. First question for the owner: is any online path acceptable.
- Play Store listing, Windows code signing or installer, Linux packages (deb, AppImage). Each is an optional gate; v1 does not depend on them.
