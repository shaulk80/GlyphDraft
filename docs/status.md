# GlyphDraft: status

Updated: 2026-11-14. Keep this file under 40 lines. History belongs in git and pull requests, not here.

## Now

M1 (clickable skeleton) is done and merged (PR #4, 2026-11-14): Home, Result and Settings screens, stub engine, FileKit file dialogs on desktop, system pickers on Android. The owner clicked through desktop (incl. the packaged app) and Android and requested no further changes (results: docs/manual-testing.md). Next: M2 (OCR with language detection, v0.1.0), see docs/plan.md.

## Releases

| Version | Date | Assets | Notes |
|---|---|---|---|
| [v0.0.1](https://github.com/shaulk80/GlyphDraft/releases/tag/v0.0.1) | 2026-11-13 | windows-x64 zip (41a74c86), linux-x64 tar.gz (8e0a1560), android apk (3ec56260); first 8 hex of SHA-256 | Pre-release, unsigned, no features. Android APK is debug-signed. |

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- Android release keystore and CI secrets: implement before v0.1.0 (decisions/0002, confirmed).
- `stacks/kotlin-multiplatform.md` for the newproject skill not written yet (owner's call).

## How things are set up

- CI: build-windows (windows-2022), build-linux and build-android (ubuntu-24.04); required checks on `main`.
- Branch protection: rulesets `protect-main` and `protect-release-tags` active since 2026-11-13. Every change goes through a PR.
- Verified: build.ps1 on all three OSes in CI; release.yml dry run and the real tag run; the published assets download anonymously, match `SHA256SUMS.txt`, Linux launcher keeps its executable bit; the published Windows zip starts and writes its log; the APK launches on an emulator (CI API 34, local API 37).
- Not verified (M1): Linux file dialogs and the Linux package (owner has WSL; deferred, CI covers build and smoke test only); Android on a real device (emulator API 37 only). Settings are in memory and reset on restart until M2.

## Decisions worth remembering

- See `docs/decisions/`.
