# GlyphDraft: status

Updated: 2026-11-20. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 and M1 are done and merged (PRs #1, #4). `v0.0.1` is a pre-release (unsigned, no features, debug-signed APK). M1 gave Home, Result and Settings screens with a stub engine; the owner clicked through desktop and Android (`docs/manual/m1.md`). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- M2: OCR with language detection, first real release v0.1.0 (`docs/plan.md`, 10 tasks). Merged: 1 spike (PP-OCRv5 on ONNX Runtime; Hebrew only via Tesseract, `docs/spikes/m2-ocr-engines.md`), 2 engine types in `core`, 3a/3b ONNX Runtime and JavaCPP Tesseract load on desktop and Android, 4 model store and download (`decisions/0007`; the apps do not use it yet), 5 PP-OCRv5 detection and recognition (`decisions/0008`; the Android image decoder comes with task 8), 6 language detection and `OnDeviceOcrEngine` (`decisions/0009`: own detector, no Lingua).
- Task 7 (Hebrew, branch `m2/task-7-hebrew-tesseract`, `decisions/0010`): `TesseractHebrewReader` (JavaCPP Tesseract, `heb`) plugged into `OnDeviceOcrEngine`. With real models (opt-in `FixtureEngineTest`, `FixtureHebrewTest`) all 11 fixtures come out in the right language at 0.0 % CER, with and without a language; Hebrew probe 0.92, others at most 0.50. Not yet run on Android (task 8). Next: task 8, wire the engine into the apps.
- Before v0.1.0: Android release keystore and CI secrets (decisions/0002, plan task 9).

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Linux file dialogs and the Linux package (CI covers build and smoke test only; the owner has WSL); Android on a real device (emulator API 37 only).
- Android: state is lost when the OS kills the background process (only rotation is handled). Settings are in memory and reset on restart until M2.
- Hand-test checklists and results: `docs/manual-testing.md` (how to run and what to click) and `docs/manual/` (one results file per milestone).
