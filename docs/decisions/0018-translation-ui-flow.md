# 0018: Translation flow in `AppController`

Date: 2026-11-23. Status: proposed (M3 task 8; the owner accepts by merging the pull request). Follows 0013 (types) and 0017 (engine wiring).

- **Problem.** The translator existed but nothing in the app state could ask for a translation, offer the models of a route, or cancel one.

## Decision

1. **State.** `AppState.target` (null: do not translate; preselected from `Settings.defaultTarget`, set with `selectTarget`, stored with `setDefaultTarget`), `AppState.translation` (the translation of the current result), `Activity.Translating`, `targets` (what the models of the catalog can reach, installed or not) and `canTranslate`. Changing the target drops a translation made for another one; picking another image or reading again drops it too.
2. **When it runs.** After a successful read, when a target is set and the engine trusts the text (`lowQuality` is false): the Result screen opens with the read text, then the translation runs and fills `translation`. `translate()` does the same later from the Result screen (after the target was changed there). The read text is never lost: a failed, cancelled or declined translation leaves it on screen.
3. **Models of a route.** The source language is only known after the read, so the translation models are asked for then, not before it. `InstallPrompt` holds a list of packs (`packs`, `totalBytes`, `then: AfterInstall`); `InstallFlow` downloads the missing ones one after the other, reports progress over all of them together, and a retry after a failure skips the packs already installed. The offer for a route appears over the Result screen; Close leaves the read text alone.
4. **Cancel.** `Translating` is cancelled like `Recognizing` (the Cancel button, the Back key): `Cancelling` frees the screen at once and whatever starts next waits for the engine (`stopping`), so two engine runs never go side by side.
5. **Messages (no exceptions).** Source equals target: "The text is already in German." (a plain notice, no translation); no route: "There is no translation from English to Japanese." (error); the translator's own failure text is shown as it comes.
6. **UI in this task.** Only what had to change to keep the screens correct: the download dialog names the packs and the total size. The target picker, the toggle between original and translation, the progress and Cancel on the Result screen and the route marks are task 9.

## Alternatives rejected

- Asking for the translation models before the read when the language is chosen: it would help only half of the cases (auto-detect is the default) and add a second path; one path after the read is simpler and the read text is already useful by itself.
- Translating a text the engine doubts (`lowQuality`): the result screen already tells the user to check the text; a translation of doubtful text would look more certain than it is.
