# GlyphDraft: plan of M3 (closed)

Full text of the closed milestone, moved out of `docs/plan.md` after v0.2.0 (published 2026-11-26). Not read at session start.

## M3 - Translation

Goal: image + target language in, translated text out.

DoD: `Translator` implemented on-device (models and license recorded in decision 0012); source language comes from M2 detection; language-pair availability visible in the UI; tested on the same fixtures.

Planned 2026-11-23, updated after the spike the same day. One branch and one pull request per task (about 800 changed lines or fewer); `docs/status.md` updated in each. Release as `v0.2.0` (pre-release) after the hand test.

Spike result (`docs/spikes/m3-translation.md`, decision 0012): OPUS-MT (Marian) int8 on the ONNX Runtime the app ships, own greedy decode loop and own Kotlin SentencePiece tokenizer, no new native library. Models are one direction each, about 110 MB. chrF on 30 own sentences: 70 to 86 for en with de, fr, es, ru; zh to en and ja to en 72; en to zh readable (experimental); he to en 82 but 366 MB; ru to he 68 (144 MB). Not usable: en to ja (8), en to he (3) and he to ru (0.4, broken exports).
- Hubs: English for everything, Russian as a second hub for Hebrew. A pair runs two or three models by a route (de to he = de, en, ru, he) and the UI says "via English" or "via English and Russian".
- Targets offered: en, de, fr, es, ru, zh, he. Sources: all eight. Japanese is never a target (no model).
- Download unit is one direction. Experimental: chrF below 60 on the reference set, or a CJK target judged by reading; every Hebrew direction until a native speaker has read it.

Tasks:
1. **Spike** (done 2026-11-23).
2. **Types in `core`** (decision 0013, done 2026-11-23). `Translator` (`suspend translate(text, from, to): Outcome<Translation>`, cancellable), `Translation` (text, from, to, route), a catalog of translation directions (a model is a direction, not a "readable language"; decide whether `ModelPack` grows a kind or a second catalog is added), `Settings.defaultTarget`. Stubs and tests; M2 screens keep working.
3. **Segmenter and tokenizer** (done 2026-11-23; `engine/.../translation`). Paragraph and sentence splitting (Latin, Cyrillic, Hebrew, CJK punctuation), rejoining OCR lines, keeping blank lines; the unigram tokenizer from the spike behind a small interface; parameterized tests.
4. **Direction manifest and routes** (done 2026-11-23, decision 0014; the Hebrew mirror is still open, owner to say where). Pinned URL at a commit, SHA-256 and size per file (as 0007); route planner over the hubs; tests with the local model server. Hebrew files come from one private uploader: mirror them (decide where) and pin the mirror.
5. **The translator** (done 2026-11-23, decision 0015). Greedy decode with and without key/value cache, one direction open at a time, cancellation, ids from the model; opt-in fixture test with chrF per direction (reference set in `engine/src/test/resources/translation`). Beam search and sentence batching were left out (see the decision).
6. **Hebrew directions** (done 2026-11-23, decision 0016). The uploader's en-he, he-ru, de-he, es-he, fr-he and he-es exports are broken (chrF 0 to 9), he-de works (71.6) but the route through English is better (75.6), so no direction was added; all Hebrew pairs are measured through the hubs (62 to 79) and kept as test cases. Native read done by the owner (`docs/manual/m3-hebrew-read.md`; experimental stays), no mirror, no own export. Decide whether he to en (366 MB) stays after task 7 shows its memory on Android.
7. **Engine wiring and Android** (done 2026-11-23, decision 0017). `OnDeviceEngine` exposes the translator and the catalog holds the translation packs; run on the emulator with a debug-only probe (memory, time, downloads); `he to en` stays optional. `AppController` takes the translator in task 8, where it is first used.
8. **UI flow** (done 2026-11-23, decision 0018). `AppController`, `AppState`: target language (null means no translation, the default), translate after the read or later from Result; `InstallFlow` also asks for the models a route needs, with the total size; cancel and Back as for a read. Stub translator; `AppControllerTest` cases.
9. **UI screens** (done 2026-11-24, decision 0019). Target picker on Home and Settings with route marks, translation/original switch, progress, copy and save of either, experimental mark, click-through tests.
10. **End to end and docs** (done 2026-11-24). Opt-in test image to translation (several routes, Latin and non-Latin sources, Hebrew); README (privacy, CC-BY credits for Helsinki-NLP, OPUS-MT and the converters, also in Settings); `architecture.md`; `CLAUDE.md` gotchas; checklist `docs/manual/m3.md`.
11. **Hand test and v0.2.0** (run 2026-11-25, repeated 2026-11-26; #54 accepted). Owner runs the checklist on desktop and the emulator (first-use download, offline afterwards, each route type, a photo, cancel); fixes; `/release 0.2.0`.

Decided by the owner 2026-11-23: M3 before the open issues (#30 parked; #8, #12, #15 are CI flakes); weak directions ship marked "experimental"; Hebrew is included. Size: the 150 MB figure becomes about 115 MB per direction, he to en (366 MB) an optional exception (decision 0012, accepted).
