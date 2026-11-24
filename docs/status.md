# GlyphDraft: status

Updated: 2026-11-24. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0, M1 and M2 are done. `v0.1.0` (text from an image: PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract, models downloaded on first use) was published on 2026-11-22 as a pre-release, after the hand test (`docs/manual/m2.md`: 14 findings, all fixed or filed). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Issues from the hand test: #28 and #29 are closed (#28: the sort order of the Android file dialog is a platform limit, in the README under Known issues); #30 (Linux window icon) is parked until seen on a native Linux desktop.
- Next milestone: M3, translation (`docs/plan.md`, 11 tasks; owner decided: M3 before the open issues, weak directions experimental, Hebrew included). OPUS-MT int8 on ONNX Runtime with an own Kotlin tokenizer (decision 0012, `docs/spikes/m3-translation.md`); English and Russian are hubs. Done: tasks 1 to 8 (types, tokenizer, manifest of 13 directions and routes, `OnDeviceTranslator` with all directions matching the spike chrF, Hebrew measured through the hubs and kept experimental, engine wiring measured on the emulator, `AppController` translation flow). Task 9 (decision 0019): target picker on Home and in Settings with route marks (via, download needed, experimental), translation and original switch on Result, progress and Cancel, copy and save of what is shown. Next: task 10 (end-to-end test, README with CC-BY credits, `docs/manual/m3.md`).

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). The Linux package was hand-tested in WSL on 2026-11-22 and works.
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
