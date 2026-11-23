# 0017: Engine wiring and the Android run

Date: 2026-11-23. Status: proposed (M3 task 7; the owner accepts by merging the pull request). Follows 0013 (types), 0014 (manifest), 0015 (translator) and 0016 (Hebrew).

- **Problem.** The translator existed but no app could reach it, and nobody had run it on Android (memory, time, downloads).

## Decision

1. **One engine object.** `OnDeviceEngine` builds one `FileModelCatalog` over `ModelManifest.packs + TranslationManifest.packs` and exposes `translator` next to `engine`, `catalog` and `installer`; `close()` also closes the translator. Both composition roots already build `OnDeviceEngine`, so they get the translator without a change of their own.
2. **`AppController` is not touched here.** It takes the `Translator` in task 8, where it is first used; a parameter that nothing reads would only be churn in nine constructor calls.
3. **Translation packs are in the catalog now**, so the Settings model list shows the 13 directions and a user can download them before the translate screens exist (task 9 groups and marks them). `InstallFlow` counted any installed pack as "a reading model is installed", so a device with only a translation pack would have tried to read without a recognizer; it now looks at readable languages, and offers the first reading pack otherwise (`anInstalledTranslationModelDoesNotCountAsAnInstalledReadingModel`).
4. **Messages.** A missing model (named, "The model for English to German is not installed."), no route and blank text come from the translator; no network and server errors come from the installer (`noNetworkIsAFailureWithAReadableMessage`). There is no cap on text length: the segmenter cuts every sentence below the model's limit, the call is cancellable, and the cost is time (below), which task 8 shows as a running state with Cancel.

## Android run (emulator API 37, x86_64, 6 cores, 4 GB, debug build, 2026-11-23)

A debug-only `TranslationProbe` (`androidApp/src/debug`, a content provider that reads `files/probe.txt` at process start and logs to the tag `GlyphDraftProbe`; no release APK has it) downloaded the models in the app and translated.

| Step | Result |
|---|---|
| Download | 107 MB in 22 s, 114 MB in 20 s, 142 MB in 33 s, 352 MB (`he to en`) in 63 s |
| en to de, 74 characters | 3.9 s |
| en to de, 954 characters (8 sentences) | 15.0 s |
| he to en, 82 characters | 3.9 s |
| de to he via en and ru (three models, 60 characters) | 7.3 s |
| Process memory (resident set) | 170 MB at start; 630 to 705 MB with a model loaded; peak 709 MB; 275 MB after `close()` |

Outputs were correct (`Das Haus ist alt, aber der Garten ist wunderschön.`, `Hello, how are you today? ...`, a Hebrew sentence for the pivot). The emulator runs on the PC's CPU; a phone is probably two to four times slower, so a page of 1000 characters may take half a minute or more there. Not measured on a real device (still open in `status.md`).

## `he to en` stays

The largest model (352 MB) did not use more memory than a small one (630 MB against 695 MB resident), and it loads and runs like the others, so decision 0012's exception stands: it stays an optional download, shown with its size.

## Not done

Real device, offline run of the whole app (task 11), progress while a long text is translated (no callback in `Translator`; add one only if the hand test shows the wait is unclear).
