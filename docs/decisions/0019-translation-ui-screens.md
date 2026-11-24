# 0019: Translation screens

Date: 2026-11-24. Status: proposed (M3 task 9; the owner accepts by merging the pull request). Follows 0018 (translation flow in `AppController`).

- **Problem.** The flow existed in `AppController` but no screen could choose a target, show a translation, or say how a pair is translated.

## Decision

1. **Target picker.** Home gets "Translate into" (a dropdown: "Don't translate" plus every language the models can reach), between the language and the read step. It is hidden while the catalog has no translation model, so the M1 and M2 screens and tests are unchanged. Settings gets "Default translation" with the same dropdown; it also preselects the target on Home.
2. **Route marks in the picker.** With a chosen source language each target says how the pair runs: `Hebrew (via Russian, download needed, experimental)`. With auto-detect the source is not known, so only the names show and a line under the picker says the route and the models are shown after the read (decision 0018). A target equal to the source says that the text will not be translated. `RouteInfo` (`ui/.../state/RouteInfo.kt`) computes via, missing models and the experimental flag from the packs; a route is experimental when any of its models is.
3. **Result screen.** A "Translate into" block (picker, Translate, progress with Cancel, a hint when a doubted read was not translated by itself) and, when a translation exists, a switch between Translation and Original, the line "Translated from English to Hebrew, via Russian" and, for an experimental route, a warning. Copy and Save as use the text that is shown; a saved translation is named `scan.de.txt`, the original `scan.txt`.
4. **Model list.** The translation models were already listed (task 7); an experimental one now carries the word "Experimental", and the download dialog says so when a route includes one.
5. **Not in this task.** The CC-BY credits for Helsinki-NLP and OPUS-MT in Settings (task 10, with the README).

## Alternatives rejected

- Marking the targets by guessing the source from the installed models when auto-detect is on: a wrong "no download needed" is worse than no mark.
- A separate screen for the translation: the original and the translation are the same text in two languages; a switch on the Result screen keeps copy and save in one place.
