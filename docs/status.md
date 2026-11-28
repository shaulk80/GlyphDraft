# GlyphDraft: status

Updated: 2026-11-28. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 to M3 are done. `v0.2.0` adds on-device translation (OPUS-MT int8 on ONNX Runtime with an own Kotlin tokenizer, English and Russian as hubs, decisions 0012 to 0021) to text from an image (`v0.1.0`); the Releases page shows what is published. Hand test: `docs/manual/m3.md` (8 findings, all fixed or accepted for 0.2.0). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

**Next step: finish M4 task 1 with the 40 delivered pages** (not started). All are there with their text (checked 2026-11-28), prepared by the photo tool into `engine/src/test/resources/fixtures/handwriting-pending/`. A page moves into `handwriting/` together with its `.txt`, manifest row and README entry; then the baseline is recorded.

- The owner consents to publishing the 15 handwritten pages (2026-11-28); the 25 AI ones were drawn with Gemini ("Auto" mode, submodel unknown). Both go into the fixtures README with the pages.
- The expected text follows the page, not the request; what to check page by page, and the AI-drawn texts: `docs/manual/m4-samples.md`.
- The score must ignore letter case on `ru-01` (written in capitals; either case is acceptable).

## M4 and issues

- M4, handwriting: twelve tasks (`docs/plan.md`, decision 0022: reader per language and cloud provider open until the spikes).
- Task 1 (fixtures and baseline): photo tool, fixture rules and baseline report (printed by `build.ps1`, on the CI summary page) are in; `he-school` reads at CER 75.9 %. Done when the baseline over all pages is recorded.
- CI flakes fixed (#8, #12, #15), clean on CI in PR #58; reopen #12 if `Attempt 1 of 3 failed` returns to the `build-windows` log.
- Issues from the hand tests: #54 (list dashes are not read) and #51 (auto-detect offers no missing reading model) are open; #30 (Linux window icon) is parked until seen on a native Linux desktop.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). Linux was hand-tested on 2026-11-22 (package in WSL) and 2026-11-25 (M3).
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
