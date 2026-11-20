# 0005: OCR and language-detection engines for M2

Date: 2026-11-20. Status: **proposed**, not accepted. The owner accepts it after the spike (M2 task 1) has measured the shortlist on real images; the "Open" list below says what the spike must settle.

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

## Costs and risks

- Models are downloaded from a host we do not run (the PaddleOCR project's release files or the model repository); URL and SHA-256 are pinned in a manifest in the repo so a changed file is refused.
- ONNX Runtime unpacks its native library to a temp folder at start, which breaks the rule "write only into the data folder". The spike checks where and whether it can be pointed at the data folder; if not, the exception is documented in CLAUDE.md as for Skiko.
- The JVM package grows by the ONNX Runtime native libraries (to be measured); the Android APK grows by its `.so` files per ABI (can be limited with ABI splits).
- Run time on a phone CPU is not known; the spike measures it on the emulator only, a real device waits for M6.

## Open (the spike must answer)

- Does any PP-OCRv5 recognizer read Hebrew? If not: Tesseract for Hebrew, as above.
- Model sizes and where the ONNX files come from (official `PaddlePaddle/*_onnx` releases or a conversion we make); licenses of each file confirmed.
- Accuracy and time per image on the fixture set for each shortlisted engine; detection accuracy on short OCR output.
- Which `onnxruntime` JVM artifact covers Windows and Linux in one dependency (not confirmed by the search done for this draft).

## Model sources (candidates for the spike; none opened or downloaded yet)

URLs come from web search results, not from opened pages. Task 1 confirms each one, reads the model card for the license, lists the exact files (model and dictionary) and records the SHA-256 of each; those values become the pinned manifest of M2 task 4. Models go into a git-ignored folder, never into the repo.

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
