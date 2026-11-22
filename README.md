# GlyphDraft

AI-powered media translator: give it an image, get the text in it, in another language if you want. Runs on-device. For Windows, Linux and Android.

**Status: version 0.1.0.** Text from an image works (printed text, language detected if you do not give it). Translation and the other inputs are still to come; see [docs/status.md](docs/status.md) and [docs/plan.md](docs/plan.md).

## What it does today

Pick an image (PNG, JPEG and other common formats), optionally say which language it is in, and get the text. Copy it or save it as a text file. Languages: English, German, French, Spanish (Latin scripts), Russian, Chinese, Japanese and Hebrew. If you do not choose a language, the app works it out from the image.

## Planned features (in order)

1. ~~Text from an image (OCR); the language is detected if you do not give it.~~ (version 0.1)
2. Translation into a language you choose.
3. Handwritten text.
4. Audio to text.
5. Android: point the camera at text and read it.

## Privacy

- **Your images stay on your device.** Recognition runs locally. The app has no account, no analytics and no crash reporting, and it never uploads an image or the text found in it.
- **The only network use is the one-time download of language models**, the first time you read a language that needs one. The app asks before it downloads and shows the size. Afterwards it works offline.

| Model pack | Download | Source |
|---|---|---|
| Latin scripts (English, German, French, Spanish) | about 13 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Russian | about 13 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Chinese and Japanese | about 22 MB | PP-OCRv5 models by PaddlePaddle, from `huggingface.co` |
| Hebrew | about 1 MB | Tesseract `tessdata_fast`, from `raw.githubusercontent.com` |

  Each file is pinned to a fixed revision and checked against its SHA-256 before it is used; a file that does not match is deleted, not installed. Those servers see your IP address and the file requested, as any download does.
- **What the app writes on your computer** goes into one data folder: the downloaded models, the unpacked native libraries, a log file (`logs/glyphdraft.log`) and a small settings file (theme and default language). Windows: `%APPDATA%\GlyphDraft`. Linux: `~/.local/share/glyphdraft` (or `$XDG_DATA_HOME/glyphdraft`). Android: the app's private storage. Delete the folder, or uninstall the app, to remove all of it. Two things on the desktop still go elsewhere: an empty `onnxruntime-java*` folder in the system temp folder (ONNX Runtime removes it on a normal exit), and the graphics library of the window toolkit (Skiko), which on Windows unpacks about 24 MB into `.skiko` in your home folder on first start (delete that folder too if you remove the app; the Linux package did not create it when checked).
- **The log file** records start-up, errors, and one line per recognition: the name of the image file, the language, the number of characters and the time. It never contains the recognized text or the image itself. It stays on your device; if you report a problem, check it for file names you do not want to share.

## Install

Download the package for your system from the [Releases page](https://github.com/shaulk80/GlyphDraft/releases) and check it against `SHA256SUMS.txt`.

- **Windows (x64):** unzip, run `GlyphDraft.exe`.
- **Linux (x64):** `tar -xzf` the archive, run `bin/GlyphDraft`.
- **Android:** open the `.apk` on the device and allow installs from your browser or file manager. It is signed with the project's own key, the same for every release, so an update installs over the previous version.

Windows and Linux packages are not signed. Windows SmartScreen will warn ("More info", then "Run anyway").

## Known issues

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

[Apache-2.0](LICENSE). Third-party models downloaded by the app have their own licenses (the models above are Apache-2.0); libraries bundled in the app are checked against `config/allowed-licenses.json` at build time.
