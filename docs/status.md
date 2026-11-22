# GlyphDraft: status

Updated: 2026-11-21. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 and M1 are done and merged (PRs #1, #4). `v0.0.1` is a pre-release (unsigned, no features, debug-signed APK). M1 gave Home, Result and Settings screens with a stub engine; the owner clicked through desktop and Android (`docs/manual/m1.md`). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- M2: OCR with language detection, first real release v0.1.0 (`docs/plan.md`, 10 tasks). Merged: 1 spike (PP-OCRv5 on ONNX Runtime; Hebrew only via Tesseract, `docs/spikes/m2-ocr-engines.md`), 2 engine types in `core`, 3a/3b ONNX Runtime and JavaCPP Tesseract load on desktop and Android, 4 model store and download (`decisions/0007`; the apps do not use it yet), 5 PP-OCRv5 detection and recognition (`decisions/0008`; the Android image decoder comes with task 8), 6 language detection and `OnDeviceOcrEngine` (`decisions/0009`: own detector, no Lingua).
- Tasks 7 (Hebrew, `decisions/0010`; all 11 fixtures at 0.0 % CER with real models) and 8a to 8c (download dialog and pickers; real engine in both composition roots; settings in `<data>/settings.properties`) are merged. CI runs the fixture tests with the real models on Windows and Linux; the owner's hand test in task 10 covers Android and the emulator (not yet clicked through on desktop or emulator).
- Task 9 (branch `m2/task-9-release-basics`): app icon (desktop window, exe, MSI/deb; Android adaptive), installer metadata, README with privacy statement, Android signing from env variables with a CI key step and signature check. **Owner step before v0.1.0:** generate the release keystore and set the four `ANDROID_*` secrets (`docs/release-signing.md`), then run the release dry run. Skiko unpacks 24 MB into `~/.skiko` (listed in README and CLAUDE.md).

- Hand test (task 10) found that auto-detect failed with "no text found" on a clean English photo when Hebrew was installed: the Hebrew probe reported 0.95 confidence for a page it read nothing of. Fixed: a probe counts only if the text is Hebrew (PR `fix/hebrew-probe-blank-text`). Other findings are in `docs/manual/m2.md` (kept on the task-10 branch).

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Linux file dialogs and the Linux package (CI covers build and smoke test only; the owner has WSL); Android on a real device (emulator API 37 only).
- Android: state is lost when the OS kills the background process (only rotation is handled). - Hand-test checklists and results: `docs/manual-testing.md` (how to run and what to click) and `docs/manual/` (one results file per milestone).
