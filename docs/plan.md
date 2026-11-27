# GlyphDraft: plan

Milestones with a goal and a Definition of Done (DoD). The current and next milestone are in detail; closed ones are one line each, and their full text (task lists, DoD) is in `docs/history/` (`plan-M0-M1.md`, `plan-M2.md`, `plan-M3.md`), which is not read at session start. Task-level detail is written when a milestone starts, not before; each task is one pull request. Order follows the brief (docs/brief.md). Keep this file under 8 KB.

## Closed milestones

- **M0 - Walking skeleton.** Build, test, package and smoke-test on Windows, Linux and Android in CI; release path proven with the `v0.0.1` pre-release; Android signing decided (decisions/0002). Done 2026-11-13 (PR #1).
- **M1 - Clickable skeleton.** Pick image, language, result, copy/save and settings on desktop and Android with a stub engine; owner clicked through both (docs/manual/m1.md). Done 2026-11-14 (PR #4).
- **M2 - OCR with language detection.** On-device engine (PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract), language detection, model download on first use, offline afterwards, settings, icons, signed Android APK; owner hand test (docs/manual/m2.md, 14 findings fixed or filed). `v0.1.0` published 2026-11-22 as a pre-release.
- **M3 - Translation.** On-device OPUS-MT on ONNX Runtime with an own tokenizer, routes through English and Russian, experimental marks, translation UI (decisions 0012 to 0021); owner hand test (docs/manual/m3.md, 8 findings fixed or accepted). `v0.2.0` published 2026-11-26 as a pre-release.
- Open from M0: `stacks/kotlin-multiplatform.md` for the newproject skill is the owner's call.

## M4 - Handwriting

Goal: handwritten text recognised.

DoD: a handwriting-capable `OcrEngine` behind the same interface (evaluate on-device models first; if quality is poor, record that and add the optional cloud engine as the first configurable backend); a handwriting fixture set with an accuracy figure reported (warning, not gate).

Planned 2026-11-27; candidates, quality bar and cloud providers: decision 0022. One branch and one pull request per task (about 800 changed lines or fewer); `docs/status.md` updated in each. Release as `v0.3.0` (pre-release) after the hand test.

Decided by the owner 2026-11-27:
- All eight languages. Owner's handwriting for en, he, ru; AI-drawn samples for de, fr, es, zh, ja.
- An online reader is accepted: opt-in, the user's own API key.
- Quality bar, mean CER per language: up to 10 % good, up to 25 % experimental, above that poor.
- A language measured only on AI-drawn samples ships experimental at best; an AI agent told to proofread may check those samples.
- TrOCR may be used. #57 before the release; #51 and #54 after M4 unless they block handwriting.
- The app offers an optional switch to correct the writer's mistakes.

Tasks (5 to 7 are confirmed, resized or dropped at the gate after tasks 2 and 3):
1. **Fixtures and baseline.** At least 5 pages per language (print-like and cursive, lined and plain paper), expected text as written (mistakes kept) and proofread against the image, consent to publish, stored upright with EXIF removed (location), small files. AI-drawn ones are marked synthetic and proofread by an AI agent. Opt-in test reports CER per language with the shipped models, never fails on accuracy; CI shows the table. Done when the baseline is recorded.
2. **On-device spike** (`docs/spikes/m4-handwriting.md`). PP-OCRv6 recognizers (also on printed fixtures), v5 against v6 detector, line finding on slanted lines; TrOCR for English; TrOCR exports where still poor (the owner installs the Python tools; hosting decided); size, time, memory.
3. **Cloud spike** (same file). Providers of decision 0022 on the same fixtures: CER, invented text, correction of the writer's mistakes, thinking levels, time, cost; the choice rests on the real (en, he, ru) samples. Claude first, through Claude Code on the owner's Pro subscription (indication only, decision 0022); Gemini on a free key before choosing; keys in environment variables. Gate: decision 0022 records the reader per language and the provider.
4. **Photo orientation on desktop.** `AwtImageDecoder` ignores EXIF orientation (from the code, not reproduced; Android applies it): failing test, then fix.
5. **Models.** Pinned manifest entries and recognizers; if v6 replaces v5 packs, handle the changed download and old files of existing installs. Printed fixtures must not get worse.
6. **Handwriting route** (only if its path differs from print): `TextStyle` in `core`, `OcrEngine` takes it, engine routing (no Hebrew probe for handwriting), language detection and `ReadQuality` on noisy reads. A TrOCR decoder, if chosen, is its own pull request first.
7. **Handwriting UI** (only with task 6): "Handwritten" switch and its default, languages offered, experimental mark, download offer, click-through tests.
8. **Cloud engine.** Decision on key storage; the chosen provider as an `OcrEngine` in `engine`; running text without line places (decision 0021); output length limit, refusals and errors as `Outcome`; fake-server tests, real call opt-in, no key in CI or logs.
9. **Cloud UI and privacy.** Online reader off by default, key and provider terms in Settings, consent before the first upload, a visible mark that the image leaves the device, README privacy.
10. **Correction** (online reader only). Optional switch, off by default; when on, one request returns the text as written and corrected (`core` type, engine). Result screen switches "As written / Corrected"; translation, copy and save use the one shown. Tests: both texts, a missing corrected part, click-through.
11. **End to end and docs.** Handwriting to translation test, emulator run (memory, time), README, `architecture.md`, `CLAUDE.md`, checklist `docs/manual/m4.md`.
12. **Hand test and v0.3.0.** Owner runs the checklist on desktop and the emulator with handwriting not in the fixtures; fixes; #57; `/release 0.3.0`.

## M5 - Audio

Goal: audio file in, text (optionally translated) out.

DoD: `SpeechRecognizer` interface and on-device implementation (e.g. Whisper family, size/speed trade-off recorded); supported audio formats listed; long files processed without freezing the UI, cancellable.

## M6 - Android camera

Goal: point the camera at text, get the text.

DoD: CameraX preview with permission flow; capture-and-recognise first, live recognition if performance allows; runs on a real device; the owner has tried it.

## Later (unscheduled, only if wanted)

- Engine settings screen and further cloud engines (M4 brings the first, for reading).
- Online translation (idea 2026-11-23, not decided): an optional `Translator` that calls a cloud service with the user's own API key, off by default, on-device stays the fallback. Gains: no gaps (en to ja, en to he), direct pairs, better quality, no model downloads. Costs: text leaves the device (breaks the privacy promise, needs an opt-in and README changes), key storage, provider terms, no CI without secrets. The 2026-11-27 yes to an online path covers reading (M4) only.
- Play Store listing, Windows code signing or installer, Linux packages (deb, AppImage). Each is an optional gate; v1 does not depend on them.
