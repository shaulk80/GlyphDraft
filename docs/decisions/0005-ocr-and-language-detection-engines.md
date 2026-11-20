# 0005: OCR and language-detection engines for M2

Date: 2026-11-20. Status: **accepted** by the owner on 2026-11-20, after the spike (M2 task 1, `docs/spikes/m2-ocr-engines.md`) measured the shortlist on 11 generated fixtures on the Windows JVM. Its results are in "Spike result" below and supersede items 2 to 4 of the original proposal.

- **Problem.** M2 needs on-device text recognition and language detection that run on Windows, Linux and Android from shared Kotlin code, cover Cyrillic, Latin, CJK and Hebrew (Hebrew is in the language picker since M1), have licenses the allow-list accepts (Apache 2.0, MIT) and download their models on first use (never committed).

## Candidates for OCR

| Candidate | Platforms | License | Cyrillic / CJK / Hebrew | Verdict |
|---|---|---|---|---|
| **PP-OCRv5 (PaddleOCR) models on ONNX Runtime** | JVM and Android from one code base; ONNX Runtime has MIT Maven artifacts for Android (`onnxruntime-android`) and a JVM one | PaddleOCR and its published weights Apache 2.0; ONNX Runtime MIT | Cyrillic: yes (`eslav` and `cyrillic` recognizers). CJK: yes (default model covers Chinese, English, Japanese). Hebrew: **not found** in the official model list | **Primary candidate.** Fits the planned `engine` module ("ONNX Runtime"), modern accuracy on photos |
| **Tesseract** (tess4j on the JVM, tesseract4android on Android) | Native library per platform, two different wrappers | Apache 2.0 (engine, tess4j and the `tessdata` files; tesseract4android to be confirmed) | All three, Hebrew included (`heb`) | **Fallback**, mainly for Hebrew. Weaker on photos and skewed text; two wrappers mean two code paths |
| ML Kit Text Recognition v2 | Android only | Proprietary (Google ML Kit terms) | Latin, Chinese, Devanagari, Japanese, Korean; no Cyrillic, no Hebrew | Rejected: Android only, license outside the allow-list, misses two required scripts |
| Windows.Media.Ocr | Windows only | OS component | Depends on installed language packs | Rejected: not cross-platform |
| Python stacks (EasyOCR, docTR, Surya) | Need a Python runtime | various, Surya has restrictive terms | | Rejected: no Python runtime in the app |

## Candidates for language detection

| Candidate | License | Notes | Verdict |
|---|---|---|---|
| **Script detection by Unicode block** (own code, a few dozen lines) | ours | Tells Cyrillic, Hebrew, Han, kana (Japanese) and Latin apart. Also needed to choose a recognizer when the user gave no language | **Always used**, first step |
| **Optimaize language-detector** (JVM) | Apache 2.0 | Pure JVM, about 74 MB RAM with all 71 built-in profiles (project README); maintenance status to check | First candidate for Latin vs Cyrillic languages inside a script |
| **Tiny Lingua** (fork of Lingua) / **Lingua** | Apache 2.0 | Better on short text; the original needs more than 1.2 GB with all languages, the fork about 150 MB (fork author's figures) | Second candidate; Lingua itself too large unless limited to our language list |
| fastText `lid.176` | believed CC BY-SA 3.0 (unverified) | Needs a native binding | Out unless the license is reviewed and added to the allow-list |
| ML Kit Language Identification | Proprietary | Android only | Rejected |

## Proposed decision (to confirm after the spike)

1. **OCR: PP-OCRv5 models (detection + recognition) on ONNX Runtime**, in the new `engine` module behind `OcrEngine`. Recognizers per script group (Latin, Cyrillic/East Slavic, Chinese-Japanese default), each downloaded separately on first use.
2. **Hebrew: Tesseract only if the spike shows PP-OCRv5 cannot read it.** Then it is a second `OcrEngine` implementation (JVM and Android wrappers) and the engine that serves Hebrew; if that costs more than one task, Hebrew moves out of v0.1.0 and the picker stops offering it. The owner decides then.
3. **Language detection:** script detection first; then Optimaize or Tiny Lingua, whichever scores better on the OCR output of the fixtures, limited to the languages the installed models support.
4. **Choosing the recognizer when no language was given:** the open design question. Options: run the default model, read the script of its output and re-run with the matching recognizer; or run every installed recognizer and keep the most confident result. The spike measures both on mixed-script fixtures.

## Spike result (2026-11-20, details in `docs/spikes/m2-ocr-engines.md`)

1. PP-OCRv5 mobile det plus the `PP-OCRv5_mobile_rec` (zh, en, ja), `latin` and `eslav` recognizers read every non-Hebrew fixture at 0 to 1.6 % CER (character error rate), 70 to 150 ms per page. Models are official `PaddlePaddle/*_onnx` repos, Apache 2.0, 4.8 MB detector and 8 to 16.5 MB per recognizer; SHA-256 recorded in the spike file. Detection needs oriented boxes (axis-aligned boxes read a 3.5 degree skew at 70 % CER).
2. **Hebrew: no PP-OCRv5 recognizer exists** (all five tried read it at 98 to 100 % CER). Tesseract `heb` reads it at 0.0 % (`tessdata_fast`, 0.96 MB). Task 7 is needed. Wrapper: **JavaCPP presets** (`org.bytedeco:tesseract`), one code path for JVM and Android, natives for Windows, Linux and Android of about 2 MB each; `tess4j` ships only a Windows DLL and is rejected.
3. **No language given:** run the Tesseract `heb` probe first if Hebrew is installed (mean confidence 93 on Hebrew, 18 to 51 elsewhere; use 80). Otherwise detect once, run each installed recognizer on the two longest lines, read all lines with the most confident one. Running the zh/en/ja model first and reading the script of its output fails for Cyrillic.
4. **ONNX Runtime** `com.microsoft.onnxruntime:onnxruntime:1.32.0` (MIT) covers Windows and Linux in one jar (55.7 MB; trim macOS symbols from the app image). It extracts 20.7 MB of DLLs to `java.io.tmpdir` on every start and leaves them; setting `java.io.tmpdir` to `<data>/tmp` in `main` keeps it inside the data folder (verified). JavaCPP extracts to `~/.javacpp/cache` (property `org.bytedeco.javacpp.cachedir`, untested).
5. **Language detection libraries:** Optimaize last released 2016 (dropped); Lingua 1.2.2 (Apache 2.0, last release 2022, 80 MB jar) is the only candidate left on Maven Central and must be restricted to our languages; decided in task 6.
6. Not measured: Linux, Android, real photographs, handwriting. On clean rendered pages Tesseract matched PP-OCRv5, so the choice of PP-OCRv5 as the main engine rests on its published photo accuracy; the M2 hand test is the check, with Tesseract for all languages as the fallback.

## Costs and risks

- Models are downloaded from a host we do not run (the PaddleOCR project's release files or the model repository); URL and SHA-256 are pinned in a manifest in the repo so a changed file is refused.
- ONNX Runtime unpacks its native library to a temp folder at start, which breaks the rule "write only into the data folder". The spike checks where and whether it can be pointed at the data folder; if not, the exception is documented in CLAUDE.md as for Skiko.
- The JVM package grows by the ONNX Runtime native libraries (to be measured); the Android APK grows by its `.so` files per ABI (can be limited with ABI splits).
- Run time on a phone CPU is not known; the spike measures it on the emulator only, a real device waits for M6.

## Open after the spike

- ONNX Runtime and JavaCPP Tesseract on Android (emulator): tasks 3 and 7.
- Whether `org.bytedeco.javacpp.cachedir` redirects the JavaCPP cache into the data folder: task 3.
- Lingua restricted to our languages: size, accuracy on OCR output: task 6.
- Accuracy on real photographs: M2 hand test (task 10).

## Model sources (confirmed in the spike on 2026-11-20)

The `PaddlePaddle/*_onnx` repos below exist and carry the Apache 2.0 tag (the `eslav`, `latin`, `cyrillic` and `PP-OCRv5_mobile_*` ones were downloaded and hashed; see the spike file); the community conversions listed as fallback were not needed. Task 4 turns the recorded SHA-256 values into the pinned manifest. Models go into a git-ignored folder, never into the repo.

Preferred: official PaddlePaddle repos on Hugging Face (Apache 2.0 expected, to be confirmed per card).

- Text detection, mobile: https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_det_onnx
- Recognition for Chinese, English and Japanese, mobile: https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_rec_onnx
- Other scripts: pattern `https://huggingface.co/PaddlePaddle/{lang}_PP-OCRv5_mobile_rec_onnx` with `latin`, `eslav`, `cyrillic`, `korean` as `{lang}`. Existence not confirmed; the org page lists what exists: https://huggingface.co/PaddlePaddle/models

Fallback only, community conversions (use only if the license is stated and matches):

- https://huggingface.co/monkt/paddleocr-onnx (`eslav/eslav_PP-OCRv5_mobile_rec.onnx`, a Latin folder, dictionaries)
- https://huggingface.co/LunarOilRig/paddleocr-onnx (mirror of the mobile detection model, about 4.8 MB)
- RapidOCR redistributes PP-OCRv5 ONNX files as well; its official repo was not confirmed.

Reference: https://www.paddleocr.ai/main/en/version3.x/algorithm/PP-OCRv5/PP-OCRv5_multi_languages.html

To settle in the spike: whether `eslav` and `cyrillic` are one model or two (sources disagree, one lists `cyrillic` at 7.7 MB) and which reads the Russian fixtures better; each recognizer needs its character dictionary file besides the `.onnx`; no source lists a Hebrew recognizer.

Tesseract data (for the Hebrew check and as a comparison): the `tessdata_fast` and `tessdata_best` repositories of the tesseract-ocr project on GitHub; exact URLs and file sizes are confirmed in task 1.

## Sources used for this draft

PaddleOCR PP-OCRv5 multilingual page (paddleocr.ai), Hugging Face PP-OCR ONNX repos, RapidOCR README (Apache 2.0, ONNX conversions of PaddleOCR models), ML Kit text recognition v2 languages page (developers.google.com/ml-kit), Maven Central pages for `onnxruntime-android` (MIT), the Lingua, Tiny Lingua and Optimaize READMEs. Statements marked "unverified" or "to confirm" were not checked in these sources.
