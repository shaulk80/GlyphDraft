# GlyphDraft: status

Updated: 2026-11-22. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 and M1 are done (PRs #1, #4); `v0.0.1` is a pre-release. M2 (OCR with language detection, first real release v0.1.0; `docs/plan.md`) has tasks 1 to 9 merged: on-device engine (PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract), model download on first use, offline use, settings file, icon, signed Android APK. Task 10 is the owner's hand test (`docs/manual/m2.md`, run 2026-11-22: sections A to J passed, 13 findings). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Merged fixes from the hand test: auto-detect failing with Hebrew installed (#23), a card when a reading looks wrong (#24, `decisions/0011`), the Hebrew blank line (#26).
- Finding 2, Ctrl+Insert, is on branch `fix/result-ctrl-insert` (the Result text is a read-only text field; tests green). If it is not merged yet: commit, PR, merge; the owner checks selection and copy on Android (no keyboard on tap). Then delete this line.
- To do: Skiko note for Linux (finding 13: `~/.skiko` was not found there, README, CLAUDE.md and `docs/architecture.md` say it is); GitHub issues for findings 1, 9 and 12 (Android open dialog forgets its folder; 0.5 s lag after Cancel; Linux window icon).
- Then, the owner: repeat C6, C9, D2 to D4 and E3 in the app; run the release dry run for 0.1.0 and repeat A3, H4, I1, I2 on it; then `/release 0.1.0`.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). The Linux package was hand-tested in WSL on 2026-11-22 and works.
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
