# Release notes

## 0.2.0 (translation)

The app now translates what it reads: give it an image and choose a language, get the text in that language. Handwriting, audio and the camera are still to come.

- Translates into English, German, French, Spanish, Russian, Chinese or Hebrew, from any of the eight reading languages. Japanese is a source only: no usable model translates into it. Choose "Translate into" on the Home screen, or translate from the Result screen afterwards; a default target can be set in Settings.
- Switch between the translation and the original; copy and save use the text you see.
- A pair without a model of its own is translated through English (into Hebrew through Russian), one model after the other. The language picker says so ("via English", "download needed") and marks the weaker directions "experimental".
- The recognized text now has an empty line between blocks, and the translation keeps the lines of the page: a wrapped paragraph comes out as one line, while headings, list items and gaps stay. E-mail addresses and web links (beginning with `http://`, `https://` or `www.`) are kept as they are.
- Everything still runs on your device. Translation models are downloaded once, one per direction (about 115 to 150 MB each; Hebrew to English 370 MB), after the app has asked; each file is checked against its SHA-256. Afterwards translation works offline. The models are OPUS-MT by Helsinki-NLP (see Credits in the README or in Settings).
- Fixed: on Android "Choose image" opens the system file browser, which keeps its folder (#28). After Cancel the progress disappears at once and the screen is free (#29).
- Windows (x64): zip, unpack and run `GlyphDraft.exe`.
- Linux (x64): tar.gz, unpack and run `bin/GlyphDraft`.
- Android: APK, signed with the project's own key; it installs over 0.1.0. The `v0.0.1` APK was signed with a debug key: uninstall it before installing this one.

**Not signed (Windows and Linux).** Windows SmartScreen shows "Windows protected your PC": choose "More info", then "Run anyway". Verify downloads with `SHA256SUMS.txt`. Privacy details: see the README.

Known limitations:

- Machine translation makes mistakes; check what matters. It is good between English and German, French, Spanish and Russian, and weaker for Chinese as a target and for everything with Hebrew (not for sensitive documents).
- A page in two languages is translated as if it were in one; crop the image to one language if that matters.
- Translation into Hebrew can lose text, such as a phone number, and write an unrelated word in its place.
- The reader can miss the dashes of a list; the translation may then join items into one paragraph (#54).
- Line breaks are judged from where the lines are, so an unusual layout can come out wrong. Columns of tables are not restored.
- Translating takes noticeably longer than reading, more so over two or three models. On the Android emulator it needed about 700 MB of memory while a model was loaded.
- Auto-detect finds Russian, Chinese, Japanese or Hebrew only after the reading model for it is installed (#51). Choose the language yourself and the app offers the download.
- Printed text only; photos with strong skew or noise may read badly.
- A wrong language choice is not corrected: the card warns, but the text stays as read.
- Android: the sort order of the open-file dialog is not kept; the app state is lost when the system kills the background process (only rotation is handled).
- Linux: the window shows a generic icon in its corner (#30).
- Android is tested on an emulator only, not on a real device.

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
