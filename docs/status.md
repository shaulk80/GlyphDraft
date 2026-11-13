# GlyphDraft: status

Updated: 2026-11-13. Keep this file under 40 lines. History belongs in git and pull requests, not here.

## Now

Project set up locally, nothing built, nothing committed, GitHub repo not created. Next: install JDK 21 and Android Studio, generate the Gradle wrapper, then M0 walking skeleton (docs/plan.md).

## Releases

| Version | Date | Assets | Notes |
|---|---|---|---|
| (none yet) | | | |

## Open

- Trademark search (USPTO, TMview, WIPO) for "GlyphDraft" not done; do it before any public launch.
- Android release signing key: decide in M0 how the self-generated key is stored (GitHub secret) before v0.1.0.

## How things are set up

- CI: build-windows, build-linux, build-android (written, never run).
- Branch protection: not set up (rulesets in .github/rulesets/, apply after the first CI run).
- Verified: nothing yet.
- Not verified: CI files, build.ps1, Linux package list, emulator smoke test, all Gradle task names.

## Decisions worth remembering

- See `docs/decisions/`.
