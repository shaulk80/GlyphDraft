# 0003: Temporary files for manual testing

Date: 2026-11-14. Status: accepted by the owner on 2026-11-14.

- **Manual testing uses `C:\Vibe\z_tmp` as the root for temporary files** (test images, the app's data folder via `GLYPHDRAFT_DATA_DIR`, saved output, result notes), not `%TEMP%`. The guide uses `C:\Vibe\z_tmp\GlyphDraftTests`.
- **Scope: manual testing only.** Automated tests keep using the JVM temp directory (`Files.createTempDirectory`), and Claude's own scratch files stay in its session scratchpad. This is not a general rule for the project.
- Why: the folder is easy to find and clean, is outside the repo (no stray files in `git status`), and is not wiped by Windows temp cleanup.
- The folder is not versioned and exists only on the owner's machine; the guide creates the subfolders it needs.
