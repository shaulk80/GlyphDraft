# GlyphDraft

AI-powered media translator: give it an image, get the text in it, in another language if you want. Runs on-device. For Windows, Linux and Android.

**Status: version 0.1.0.** Text from an image works (printed text, language detected if you do not give it). Translation into another language is built on `main` and waits for its hand test (version 0.2.0); the other inputs are still to come. See [docs/status.md](docs/status.md) and [docs/plan.md](docs/plan.md).

## What it does today

Pick an image (PNG, JPEG and other common formats), optionally say which language it is in, and get the text. Copy it or save it as a text file. Languages: English, German, French, Spanish (Latin scripts), Russian, Chinese, Japanese and Hebrew. If you do not choose a language, the app works it out from the image.

**Translation (on `main`, not in 0.1.0).** Choose "Translate into" on the Home screen, or translate from the Result screen afterwards, and switch between the translation and the original; copy and save use the text you see. Targets: English, German, French, Spanish, Russian, Chinese and Hebrew; sources: all eight (Japanese is never a target, no usable model exists). The app translates between two languages other than English through English (and Hebrew through Russian), one model after the other, and says so in the picker ("via English", "download needed"). Quality: good between English and German, French, Spanish and Russian; weaker for Chinese as a target and for everything with Hebrew, which carry the mark "experimental" (Hebrew was read by a native speaker: fine for a hobby translation, not for sensitive documents). E-mail addresses and web links (beginning with `http://`, `https://` or `www.`) are kept exactly as they are. Machine translation makes mistakes; check what matters.

## Planned features (in order)

1. ~~Text from an image (OCR); the language is detected if you do not give it.~~ (version 0.1)
2. Translation into a language you choose (built, hand test pending; version 0.2).
3. Handwritten text.
4. Audio to text.
5. Android: point the camera at text and read it.

## Privacy

- **Your images stay on your device.** Recognition runs locally. The app has no account, no analytics and no crash reporting, and it never uploads an image or the text found in it.
- **The only network use is the one-time download of language models**, the first time you read or translate a language that needs one. The app asks before it downloads and shows the size. Afterwards it works offline.

| Model pack | Download | Source |
|---|---|---|
| Latin scripts (English, German, French, Spanish) | about 13 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Russian | about 13 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Chinese and Japanese | about 22 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Hebrew | about 1 MB | Tesseract `tessdata_fast`, from `raw.githubusercontent.com` |
| Translation, one direction (for example English to German) | 113 to 149 MB each; Hebrew to English 370 MB | OPUS-MT models by Helsinki-NLP in the ONNX conversions of Xenova (Hebrew: R4kSo1997), from `huggingface.co` |

  Each file is pinned to a fixed revision and checked against its SHA-256 before it is used; a file that does not match is deleted, not installed. Those servers see your IP address and the file requested, as any download does.
- **What the app writes on your computer** goes into one data folder: the downloaded models, the unpacked native libraries, a log file (`logs/glyphdraft.log`) and a small settings file (theme and default language). Windows: `%APPDATA%\GlyphDraft`. Linux: `~/.local/share/glyphdraft` (or `$XDG_DATA_HOME/glyphdraft`). Android: the app's private storage. Delete the folder, or uninstall the app, to remove all of it. Two things on the desktop still go elsewhere: an empty `onnxruntime-java*` folder in the system temp folder (ONNX Runtime removes it on a normal exit), and the graphics library of the window toolkit (Skiko), which on Windows unpacks about 24 MB into `.skiko` in your home folder on first start (delete that folder too if you remove the app; the Linux package did not create it when checked).
- **The log file** records start-up, errors, and one line per recognition: the name of the image file, the language, the number of characters and the time. It never contains the recognized text, the translated text or the image itself. It stays on your device; if you report a problem, check it for file names you do not want to share.

## Credits

Translation runs on [OPUS-MT](https://github.com/Helsinki-NLP/Opus-MT) models by the Helsinki-NLP group (University of Helsinki), licensed CC-BY 4.0 (some model cards say Apache 2.0; both are treated as CC-BY 4.0 here). They are converted to ONNX and quantized to int8 by [Xenova](https://huggingface.co/Xenova) and, for the two Hebrew models, by the community uploader R4kSo1997 (the uploader gives no license text; the base models' licenses apply, this is the reading of a non-lawyer). Nothing else was changed in the models. The app shows the same credit in Settings. Text recognition uses PP-OCRv5 by PaddlePaddle and Tesseract (both Apache-2.0).

## Install

Download the package for your system from the [Releases page](https://github.com/shaulk80/GlyphDraft/releases) and check it against `SHA256SUMS.txt`.

- **Windows (x64):** unzip, run `GlyphDraft.exe`.
- **Linux (x64):** `tar -xzf` the archive, run `bin/GlyphDraft`.
- **Android:** open the `.apk` on the device and allow installs from your browser or file manager. It is signed with the project's own key, the same for every release, so an update installs over the previous version.

Windows and Linux packages are not signed. Windows SmartScreen will warn ("More info", then "Run anyway").

## Known issues

- **A page in two languages is translated as if it were in one.** The app takes one source language per image, so text in another language goes through the same model and can lose its meaning. Crop the image to one language if that matters.
- **Android: the sort order of the file dialog is not kept.** "Choose image" opens the system file browser, which keeps the last folder but not the sort order you picked; that setting belongs to the system and the app cannot change it.

## Build

Needs JDK 21 and, for Android, the Android SDK (Android Studio installs it). Then:

```powershell
.\scripts\build.ps1                  # licenses, build, test, package, checksums, smoke test (this OS)
.\scripts\build.ps1 -Target android  # APK
.\gradlew :desktopApp:run            # run the desktop app
```

Packages are written to `dist/`. Without the release key (only the owner has it), the Android APK is signed with a debug key; see [docs/release-signing.md](docs/release-signing.md).

## License

[Apache-2.0](LICENSE). Third-party models downloaded by the app have their own licenses (the reading models above are Apache-2.0, the translation models CC-BY 4.0, see Credits); libraries bundled in the app are checked against `config/allowed-licenses.json` at build time.
