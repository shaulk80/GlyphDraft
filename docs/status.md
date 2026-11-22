# GlyphDraft: status

Updated: 2026-11-22. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0, M1 and M2 are done. `v0.1.0` (text from an image: PP-OCRv5 on ONNX Runtime, Hebrew through Tesseract, models downloaded on first use) was published on 2026-11-22 as a pre-release, after the hand test (`docs/manual/m2.md`: 14 findings, all fixed or filed). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- Open issues from the hand test: #28 (Android open dialog forgets its folder), #29 (0.5 s lag after Cancel), #30 (Linux window icon).
- Next milestone: M3, translation (`docs/plan.md`). Plan it first; the owner decides the order of M3 and the open issues.

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Android on a real device (emulator API 37 only). The Linux package was hand-tested in WSL on 2026-11-22 and works.
- Android: state is lost when the OS kills the background process (only rotation is handled).
- Hand-test how-to and results: `docs/manual-testing.md` and `docs/manual/` (one file per milestone).
