# Release notes

## 0.1.0 (text from an image)

The first real release: give it an image, get the text in it. Translation, handwriting, audio and the camera are still to come.

- Reads printed text from an image (PNG, JPEG and other common formats) on Windows, Linux and Android. Languages: English, German, French, Spanish, Russian, Chinese, Japanese and Hebrew.
- If you do not choose a language, the app works it out from the image. A card on the Result screen warns when a reading looks wrong, for example when the language models are missing or another language was chosen.
- Copy the text or save it as a `.txt` file. Hebrew is shown right to left.
- Everything runs on your device. The only network use is a one-time download of the language models (about 1 to 22 MB per pack) the first time a language needs one; the app asks first. Afterwards it works offline. No account, no analytics.
- Windows (x64): zip, unpack and run `GlyphDraft.exe`.
- Linux (x64): tar.gz, unpack and run `bin/GlyphDraft`.
- Android: APK, signed with the project's own key, so later releases install over this one. The `v0.0.1` APK was signed with a debug key: uninstall it before installing this one.

**Not signed (Windows and Linux).** Windows SmartScreen shows "Windows protected your PC": choose "More info", then "Run anyway". Verify downloads with `SHA256SUMS.txt`. Privacy details: see the README.

Known limitations:

- Printed text only; photos with strong skew or noise may read badly, and the layout (spacing, columns) is not kept.
- A wrong language choice is not corrected: the card warns, but the text stays as read.
- Android: the open-file dialog does not remember its folder (#28); the progress indicator stays about half a second after Cancel (#29); the app state is lost when the system kills the background process (only rotation is handled).
- Linux: the window shows a generic icon in its corner (#30).
- Android is tested on an emulator only, not on a real device.

## 0.0.1 (walking skeleton)

Pre-release that only proves the build and release path. It has no OCR yet.

- Windows (x64): zip, unpack and run `GlyphDraft.exe`.
- Linux (x64): tar.gz, unpack and run `bin/GlyphDraft`.
- Android: APK, sideload it.

**Not signed.** Windows SmartScreen shows "Windows protected your PC": choose "More info", then "Run anyway". Android asks to allow installs from this source. Verify downloads with `SHA256SUMS.txt`.

Known limitations: no features yet.
