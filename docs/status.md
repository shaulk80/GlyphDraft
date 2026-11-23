# GlyphDraft: status

Updated: 2026-11-23. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0, M1 and M2 are done. `v0.1.0` (text from an image: PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract, models downloaded on first use) was published on 2026-11-22 as a pre-release, after the hand test (`docs/manual/m2.md`: 14 findings, all fixed or filed). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Issues from the hand test: #28 and #29 are closed (#28: the sort order of the Android file dialog is a platform limit, in the README under Known issues); #30 (Linux window icon) is parked until seen on a native Linux desktop.
- Next milestone: M3, translation (`docs/plan.md`, 11 tasks; owner decided: M3 before the open issues, weak directions experimental, Hebrew included). OPUS-MT int8 on ONNX Runtime with an own Kotlin tokenizer (decision 0012, `docs/spikes/m3-translation.md`); English and Russian are hubs. Done: tasks 1 to 4 (spike, types in `core`, segmenter and tokenizer, manifest of 13 directions and routes; the Hebrew files are not mirrored, by choice) and task 5 (decision 0015): `OnDeviceTranslator` runs a route one direction at a time, greedy decoding, not wired into the apps yet. With the real models all 13 directions and 4 pivot pairs match the spike chrF; `he to en` is 78.9, not 82.5 (the spike used a wrong end id). Also fixed: the OCR engine counted installed translation packs as reading packs. Task 6 (decision 0016): six more Hebrew exports of the uploader are broken and the seventh (he to de) is worse than the route through English, so no direction was added; every Hebrew pair is measured through the hubs (chrF 62 to 79). The owner read the Hebrew outputs (`docs/manual/m3-hebrew-read.md`): fine for everyday use, not for sensitive text, so the experimental mark stays; no mirror, no own export. Task 7 (decision 0017): `OnDeviceEngine` exposes the translator, the catalog holds the 13 translation packs (they show in the Settings model list), a device with only translation packs is still offered a reading pack; measured on the emulator (peak 709 MB, 954 characters en to de in 15 s). Task 8 (decision 0018): `AppController` takes the translator; target language, `translate()`, route models offered after the read with the total size, cancel; no screens for it yet (task 9: target picker, toggle, progress, marks). Next: task 9.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). The Linux package was hand-tested in WSL on 2026-11-22 and works.
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
