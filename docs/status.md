# GlyphDraft: status

Updated: 2026-11-13. Keep this file under 40 lines. History belongs in git and pull requests, not here.

## Now

M0 in progress. PR #1 (`m0`) has all three CI jobs green. Left for M0: merge the PR, apply the rulesets, `v0.0.1` dry run and release (docs/plan.md).

## Releases

| Version | Date | Assets | Notes |
|---|---|---|---|
| (none yet) | | | |

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- Android release keystore and CI secrets: implement before v0.1.0 (decisions/0002, confirmed).
- Android status bar text is white on the light background (edge-to-edge); fix in M1.

## How things are set up

- CI: build-windows (windows-2022), build-linux and build-android (ubuntu-24.04); green on PR #1.
- Branch protection: not set up (rulesets in .github/rulesets/; apply after the PR is merged).
- Verified: build.ps1 on Windows, Linux and Android in CI (license check, tests, package, checksums, desktop smoke tests, APK launch on an API 34 emulator); locally on Windows and an API 37 emulator.
- Not verified: release.yml (needs the dry run after merge), upload of packages from `main`, SHA256SUMS merge.

## Decisions worth remembering

- See `docs/decisions/`.
