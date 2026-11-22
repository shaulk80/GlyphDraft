# GlyphDraft: status

Updated: 2026-11-22. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 and M1 are done (PRs #1, #4); `v0.0.1` is a pre-release. M2 (OCR with language detection, first real release v0.1.0; `docs/plan.md`) has tasks 1 to 9 merged: on-device engine (PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract), model download on first use, offline use, settings file, icon, signed Android APK. Task 10 is the owner's hand test (`docs/manual/m2.md`, run 2026-11-22: sections A to J passed, 13 findings). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Merged fixes from the hand test: auto-detect failing with Hebrew installed (#23), a card when a reading looks wrong (#24, `decisions/0011`), the Hebrew blank line (#26), Ctrl+Insert copy (#27), the Skiko note for Linux (finding 13). Findings 1, 9 and 12 are issues #28, #29 and #30.
- The repeat run passed on desktop and Android (C6 to E3, Android copy), including the Hebrew direction fix (finding 14, #32); see `docs/manual/m2.md`.
- Release 0.1.0 is in progress: version and notes in the release pull request; then the release dry run (`release.yml` on `main`), the owner repeats A3, H4, I1, I2 on its packages, the owner pushes the tag `v0.1.0` and publishes the draft.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). The Linux package was hand-tested in WSL on 2026-11-22 and works.
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
