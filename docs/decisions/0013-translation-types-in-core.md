# 0013: Translation types in `core`

Date: 2026-11-23. Status: accepted. Follows 0006 (engine types in core) and 0012 (translation engine).

- **Problem.** M3 needs a `Translator` seam and a way to say which translation models exist and are installed. A model is one direction (0012), not a "readable language", and the download, install and model-list machinery of M2 (0007) should serve it without a second copy.

## Decision

1. **`Translator.translate(text, from, to): Outcome<Translation>`**, suspending and cancellable, like `OcrEngine`. Expected problems (blank text, no route, model not installed) are `Outcome.Failure` with a user message. Same source and target returns the text unchanged.
2. **`Translation(text, from, to, route)`.** `route` is the list of languages the text went through, source first and target last, so the UI can say "via English" (`via` is the middle part). The route is computed by the engine (task 4), not by the UI.
3. **A translation model is a `ModelPack`** with a `direction: TranslationDirection?` (null for recognition packs), an empty `languages` set and an `experimental` flag. No second catalog and no second installer: `ModelCatalog`, `ModelInstaller`, `PackManifest` and the model list work on both. `languages` stays empty so `usableLanguages()` and `knownLanguages()` (the pickers of readable languages) are unchanged. New helpers: `translationPacks()`, `packFor(direction)`, `usableDirections()`.
4. **`Settings.defaultTarget: Language?`**, null meaning "do not translate" (the default, so M2 behaviour is unchanged). Stored as `defaultTarget` in `settings.properties`; an unknown code reads as null.
5. **`StubTranslator`** (marks the text with the target code) for the UI work in tasks 8 and 9. The stub catalog gets no translation packs yet, so M2 screens do not change.

## Not decided here

Which pairs are offered as source or target (the matrix of 0012) and how the route is planned: task 4, in `engine`, next to the manifest.

## Alternatives rejected

- A separate `TranslationCatalog` and installer: doubles the install flow, progress, cancel and model-list code for no gain.
- A `kind` enum on `ModelPack`: the direction already tells the kind, and a second field could disagree with it.
