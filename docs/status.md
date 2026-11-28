# GlyphDraft: status

Updated: 2026-11-28. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 to M3 are done. `v0.2.0` adds on-device translation (OPUS-MT int8 on ONNX Runtime with an own Kotlin tokenizer, English and Russian as hubs, decisions 0012 to 0021) to text from an image (`v0.1.0`); the Releases page shows what is published. Hand test: `docs/manual/m3.md` (8 findings, all fixed or accepted for 0.2.0). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

**Next step: M4 task 2, the on-device spike** (`docs/plan.md`; not started).

## M4 and issues

- M4, handwriting: twelve tasks (`docs/plan.md`, decision 0022: reader per language and cloud provider open until the spikes).
- Task 1 (fixtures and baseline) is done: 41 pages, baseline in decision 0022 (2026-11-28): English 3.1 % CER, Russian 20.9, Hebrew 64.1; AI-drawn de, es, zh, ja under 1, fr 9.7.
- CI flakes fixed (#8, #12, #15), clean on CI in PR #58; reopen #12 if `Attempt 1 of 3 failed` returns to the `build-windows` log.
- Issues from the hand tests: #54 (list dashes are not read) and #51 (auto-detect offers no missing reading model) are open; #30 (Linux window icon) is parked until seen on a native Linux desktop.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). Linux was hand-tested on 2026-11-22 (package in WSL) and 2026-11-25 (M3).
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
