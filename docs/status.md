# GlyphDraft: status

Updated: 2026-11-13. Keep this file under 40 lines. History belongs in git and pull requests, not here.

## Now

M0 in progress on branch `m0` (nothing committed yet, GitHub repo not created). The skeleton builds, tests pass, and Windows and Android packages work locally. Left for M0: first CI run (Linux unverified), branch protection, `v0.0.1` dry run and release (docs/plan.md).

## Releases

| Version | Date | Assets | Notes |
|---|---|---|---|
| (none yet) | | | |

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- Android release key: proposal in decisions/0002, owner to confirm before v0.1.0.
- Android status bar text is white on the light background (edge-to-edge); fix in M1.

## How things are set up

- CI: build-windows, build-linux, build-android (written, never run on GitHub).
- Branch protection: not set up (rulesets in .github/rulesets/, apply after the first CI run).
- Verified locally (Windows 10, JDK 21): `scripts/build.ps1 -Target windows` and `-Target android` exit 0 (license check, tests, package, checksums); the Windows package starts and writes its log; the APK installs, launches and renders on the API 37 emulator (`smoke-android.sh` passed).
- Not verified: everything on Linux, the CI workflows, the emulator step on GitHub runners (API 34), release.yml.

## Decisions worth remembering

- See `docs/decisions/`.
