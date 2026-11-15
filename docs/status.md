# GlyphDraft: status

Updated: 2026-11-15. Keep this file under 3 KB. History belongs in git, pull requests and the Releases page, not here.

## Now

M0 and M1 are done and merged (PRs #1, #4). `v0.0.1` is a pre-release (unsigned, no features, debug-signed APK). M1 gave Home, Result and Settings screens with a stub engine, FileKit file dialogs on desktop and system pickers on Android; the owner clicked through desktop (including the packaged app) and Android and asked for no further changes (`docs/manual/m1.md`). CI builds, tests, packages and smoke-tests on Windows, Linux and Android; `main` and release tags are protected.

## Next

- M2: OCR with language detection, first real release v0.1.0 (`docs/plan.md`).
- Before v0.1.0: Android release keystore and CI secrets (decisions/0002).

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- `stacks/kotlin-multiplatform.md` for the newproject skill is not written (owner's call).
- Not verified: Linux file dialogs and the Linux package (CI covers build and smoke test only; the owner has WSL); Android on a real device (emulator API 37 only).
- Android: state is lost when the OS kills the background process (only rotation is handled). Settings are in memory and reset on restart until M2.
- Hand-test checklists and results: `docs/manual-testing.md` (how to run and what to click) and `docs/manual/` (one results file per milestone).
