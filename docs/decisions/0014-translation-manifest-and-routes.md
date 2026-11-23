# 0014: Translation manifest and routes

Date: 2026-11-23. Status: proposed (M3 task 4; the owner accepts by merging the pull request). Follows 0007 (model store), 0012 (translation engine) and 0013 (types in core).

- **Problem.** The translation models must be listed, pinned and installable like the recognition packs, and the app must know which pairs it can translate and through which models.

## Decision

1. **`TranslationManifest`** (engine, next to `ModelManifest`): one `PackManifest` per direction, pack id `mt-<from>-<to>`, five files each: `config.json`, `vocab.json`, `tokenizer.json`, the int8 encoder and decoder (`source.spm` and the other ONNX variants are not downloaded). URL at a commit hash, SHA-256 and size as in 0007; all 65 hashes and sizes were checked against the hub (LFS SHA-256, git blob id for the small files) at the pinned revisions. On disk `<models>/mt/<from>-<to>/<file name as in the repository>`; no file is shared between directions. `TranslationManifest.filesOf(direction)` gives the named files and `decoderHasCache` (merged decoder with key/value cache for the `Xenova` models, plain decoder for the Hebrew ones) to the translator of task 5. The composition root passes `ModelManifest.packs + TranslationManifest.packs` to the catalog (task 7).
2. **13 directions:** en to and from de, fr, es, ru, zh; ja to en; he to en; ru to he. Experimental: en to zh and both Hebrew directions (0012). Not listed: en to ja, en to he, he to ru and the other Hebrew exports (task 6).
3. **Routes are computed from the directions that exist** (`TranslationRoutes`, shortest chain, ties by catalog order), not from a hand-written table. English becomes the hub and Russian the second hub of Hebrew because that is the shape of the graph (de to he = de, en, ru, he; he to ru = he, en, ru). A new direction (task 6) changes the routes without a code change. `ModelCatalog.packsForRoute(from, to)` names the models a pair needs, so the install flow (task 8) can total the download.
4. **`TranslationRoutes` lives in `core`, not in `engine`** as 0013 foresaw: it is pure logic over `TranslationDirection`, and the UI (tasks 8 and 9) needs it for "via English", "download needed" and for hiding impossible targets, while `ui` must not depend on `engine`.
5. **Sizes.** A download is 113 to 121 MB per direction for the Xenova models (the ONNX files of the spike, 106 to 113 MB, plus 7 to 11 MB of tokenizer and vocabulary), `ja-en` 116 MB, `ru-he` 149 MB, `he-en` 370 MB. The 115 MB of 0012 is "about"; the test ceiling is 125 MB for everything except Hebrew.
6. **Hebrew mirror: not done.** The two Hebrew directions point at the uploader's repositories at a commit. A changed file is refused by its hash; a removed repository breaks first installs of those two directions. A mirror (a repository or release asset of the project) needs an owner decision on where; open for task 6.
7. **Tests.** Manifest rules (pins, folders, ids, sizes, experimental set, the shipping set) in `TranslationManifestTest`; a translation pack installed from the local server into `mt/`; route cases in `TranslationRoutesTest` (core). The opt-in `RealDownloadTest` asks the real servers (HEAD) for the size of every file instead of downloading 1.2 GB.

## Alternatives rejected

- A hand-written route table: duplicates what the directions already say and goes stale when task 6 adds a direction.
- One pack per language pair of both directions: breaks the size ceiling (0012).
