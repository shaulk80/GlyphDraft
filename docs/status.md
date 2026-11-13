# GlyphDraft: status

Updated: 2026-11-13. Keep this file under 40 lines. History belongs in git and pull requests, not here.

## Now

M0 (walking skeleton) is done: `v0.0.1` pre-release is published. Next: M1, the clickable skeleton (docs/plan.md). Start a new branch `m1`.

## Releases

| Version | Date | Assets | Notes |
|---|---|---|---|
| [v0.0.1](https://github.com/shaulk80/GlyphDraft/releases/tag/v0.0.1) | 2026-11-13 | windows-x64 zip (41a74c86), linux-x64 tar.gz (8e0a1560), android apk (3ec56260); first 8 hex of SHA-256 | Pre-release, unsigned, no features. Android APK is debug-signed. |

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- Android release keystore and CI secrets: implement before v0.1.0 (decisions/0002, confirmed).
- Android status bar text is white on the light background (edge-to-edge); fix in M1.
- `stacks/kotlin-multiplatform.md` for the newproject skill not written yet (owner's call).

## How things are set up

- CI: build-windows (windows-2022), build-linux and build-android (ubuntu-24.04); required checks on `main`.
- Branch protection: rulesets `protect-main` and `protect-release-tags` active since 2026-11-13. Every change goes through a PR.
- Verified: build.ps1 on all three OSes in CI; release.yml dry run and the real tag run; the published assets download anonymously, match `SHA256SUMS.txt`, Linux launcher keeps its executable bit; the published Windows zip starts and writes its log; the APK launches on an emulator (CI API 34, local API 37).
- Not verified: the published Linux tar.gz and APK were not started by hand (CI smoke-tested the same build); no real Android device; the desktop window was not looked at after the final release.

## Decisions worth remembering

- See `docs/decisions/`.
