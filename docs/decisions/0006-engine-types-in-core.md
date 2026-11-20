# 0006: Engine types in `core` for M2

Date: 2026-11-20. Status: proposed (M2 task 2; the owner accepts by merging the pull request).

- **Problem.** M2 needs the seams between the UI, the engines and the model downloads before any engine exists, so that tasks 3 to 8 can be built and tested against them.
- **Image stays encoded bytes (`ImageInput`).** Decoding to pixels is platform code (`BufferedImage` on the JVM, `Bitmap` on Android) and `core` has none. The `engine` module decodes behind its own small `expect`/`actual` (task 5) and the pickers keep handing over file bytes as in M1. A decoded bitmap type in `core` would force every picker and test to decode first, and would still need two decoders.
- **`OcrEngine` keeps its signature** (`recognize(image, languageHint?)`); the contract is now written down: a hint without an installed model is a `Failure`, and with no hint the engine detects among the installed languages. The UI checks the catalog first and offers the download, so no failure subtype is needed. `Language` and `RecognitionResult` are unchanged; a script field on `Language` is added in task 6 when the detector needs it.
- **`LanguageDetector`**: `suspend detect(text, candidates): LanguageGuess?`. Candidates are the installed languages, so a detector never answers with a language that cannot be read; null means "cannot tell".
- **`ModelCatalog`**: `packs` (all known) and `installedPackIds` (`StateFlow`). A `ModelPack` is one downloadable unit with the languages it serves and its size, so the UI can say "Russian, 13 MB" and the picker can list `knownLanguages()`; `usableLanguages()` is what works offline now. The mapping of packs to PP-OCRv5 files (detector shared by all packs) belongs to task 4.
- **`ModelInstaller`**: `suspend install(pack, onProgress): Outcome<Unit>`. Progress by callback, cancellation by coroutine cancellation (implementations delete partial files). A `Flow` of events was considered; a callback plus a suspending result is simpler for one download at a time.
- **Stubs** (`core.stub`) back tests and the skeleton; the apps still use `StubOcrEngine` until task 8.
