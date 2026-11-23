# 0012: Translation engine for M3

Date: 2026-11-23. Status: **proposed**, waits for the owner's answers on the two questions at the end. Evidence: `docs/spikes/m3-translation.md`.

- **Problem.** M3 needs on-device translation between the eight languages of the pickers on Windows, Linux and Android from shared Kotlin code, with no new native library and a license that allows shipping.

## Decision (proposed)

1. **Engine: OPUS-MT (Marian) models, int8 ONNX, on the ONNX Runtime the app already ships**, with our own greedy decode loop (key/value cache, merged decoder) and our own SentencePiece unigram tokenizer in Kotlin (no JNI). Models come from the `Xenova/opus-mt-*` repos, one pair of files per direction, downloaded on first use as in 0007 (pinned URL at a commit, SHA-256, size).
2. **English is the pivot.** Direct models exist only to and from English. Any other pair runs two models (source to en to target); the UI says "via English". It costs 3 to 8 chrF and twice the time.
3. **Download unit: one direction** (106 to 113 MB), not a pack of two. A pack of both directions is 213 to 226 MB and breaks the 150 MB working ceiling. The UI may still group the two directions of a language and show the sum.
4. **Experimental mark** (threshold fixed here): a direction with chrF below 60 on the reference set, or a CJK target judged by reading, is marked experimental. On the current evidence: en to zh is experimental, all others of the shipping set are not.
5. **Not shipped:** English to Japanese (chrF 8.3, the only OPUS-MT model is unusable) and English to Hebrew (no working ONNX model; export it ourselves later). Japanese and Hebrew work as sources. The pickers must therefore offer ja and he as "from" only; any pair with ja or he as target is hidden or disabled with a reason.
6. **Hebrew** (owner wants it included, 2026-11-23). As a source: he to en needs the 366 MB `tc-big` model. As a target: **ru to he works directly (chrF 68.2, 144 MB)**, so Hebrew as a target is reached as X to en to ru to he (en to ru to he scored 67.3), i.e. Russian is a second hub for Hebrew. he to ru and en to he exports are broken; to be fixed by our own export, or by another uploader's model after a test. Everything Hebrew comes from one private uploader (no license text): mirror and pin it, and treat the Hebrew directions as experimental until a native speaker has read the output.
7. **Attribution:** CC-BY 4.0 for the models: a credits line naming Helsinki-NLP, OPUS-MT and the Xenova conversions in the README and in the app (Settings).
8. Decode loop is greedy first; beam search (4) is an option in task 5 if it is cheap.

## Alternatives rejected

- Bergamot/Firefox models: native Marian build per platform. CTranslate2: no JVM binding. M2M100, NLLB, MADLAD: 750 MB and more, NLLB is non-commercial. ML Kit: Android only, proprietary.

## Costs and risks

- About 110 MB per direction and about 500 MB of memory while a direction is loaded: one direction at a time, close the session after use; the emulator must confirm it (task 6).
- Models are hosted by third parties (Xenova on the Hugging Face hub): pinned by hash, a removed repo breaks first-time installs; a mirror is the later fix.
- Quality was measured on 30 clean one-sentence lines of our own; real OCR text will score lower.

## Questions for the owner

1. The 150 MB ceiling was meant per pack of both directions. OPUS-MT gives about 110 MB per direction. Accept **per direction** as the unit (a language is then about 220 MB if both ways are installed)? If not, the alternative is to do the vocabulary re-export ourselves (about 30 to 40 MB less per direction, needs Python and Optimum, not installed) and still not reach 150 MB for two directions.
2. Hebrew: ship **he to en only at 366 MB** (good quality, large), or leave Hebrew out of translation in v0.2.0 (OCR keeps reading Hebrew)? And is "ja and he as source only" acceptable for v0.2.0?
