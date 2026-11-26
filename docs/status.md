# GlyphDraft: status

Updated: 2026-11-26. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 to M3 are done. `v0.2.0` adds on-device translation (OPUS-MT int8 on ONNX Runtime with an own Kotlin tokenizer, English and Russian as hubs, decisions 0012 to 0021) to text from an image (`v0.1.0`); the Releases page shows what is published. Hand test: `docs/manual/m3.md` (8 findings, all fixed or accepted for 0.2.0). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Before the next milestone: the CI flakes #8, #12 and #15.
- Next milestone: M4, handwriting (`docs/plan.md`); plan it into tasks first.
- Issues from the hand tests: #54 (list dashes are not read) and #51 (auto-detect offers no missing reading model) are open; #30 (Linux window icon) is parked until seen on a native Linux desktop.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). Linux was hand-tested on 2026-11-22 (package in WSL) and 2026-11-25 (M3).
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
