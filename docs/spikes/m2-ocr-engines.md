# M2 task 1: OCR engine spike

Date: 2026-11-20. Code: `spikes/ocr` (stand-alone Gradle build, not part of the product build; run `.\gradlew -p spikes/ocr run --args="bench"` after downloading the models into `data/spike-models`). Fixtures: `engine/src/test/resources/fixtures`. Everything ran on the JVM on Windows 10 (x64, 4 ORT threads); Linux, Android and phone speed are not measured (see "Not measured").

## Fixtures

11 generated PNG pages (1100 px wide, 2 to 3 lines each) with the expected text in a `.txt` next to each; `manifest.tsv` lists name, language, script. Rendered with Windows fonts through Java 2D; the sentences are written for this project or are public-domain phrases (pangrams, a line from Soseki's 1905 novel), so the files are ours to commit (788 KB). `README.md` in the folder says so.

| Fixture | Language, script | Condition |
|---|---|---|
| en-clean, de-clean, ru-clean, zh-clean, ja-clean, he-clean | en, de (Latin); ru (Cyrillic); zh (Han); ja (Han and kana); he (Hebrew) | clean |
| mixed-clean | en + ru in one image | clean |
| en-skew | en | rotated 3.5 degrees |
| ru-noise, zh-noise | ru, zh | rotated 1.5 degrees, Gaussian noise |
| de-blur | de | blur and low contrast |

Limit: these are rendered, not photographed, and the fonts are standard. Real photos (perspective, shadows, handwriting-like fonts) will score worse; M2 task 10 adds the owner's own hand test. Error measure: character error rate (CER), edit distance over expected length, all whitespace ignored.

## PP-OCRv5 on ONNX Runtime

Models (all Apache 2.0 per the model cards, from `huggingface.co/PaddlePaddle/<name>_onnx`, file `inference.onnx` plus `inference.yml`; the yml holds the character dictionary and the pre/post-processing settings):

| Model | `.onnx` bytes | SHA-256 of `.onnx` |
|---|---|---|
| PP-OCRv5_mobile_det | 4,826,518 | a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d |
| PP-OCRv5_mobile_rec (zh, en, ja) | 16,534,782 | da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092 |
| latin_PP-OCRv5_mobile_rec | 8,042,023 | 7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498 |
| eslav_PP-OCRv5_mobile_rec | 7,887,627 | b3018ef2b09a0250b6e0c8e871c927098363e5fd4df890cc68e8358eb0aaf1bd |
| cyrillic_PP-OCRv5_mobile_rec | 8,048,799 | 5371ee1ddaa7983cc62d0818d99e982b6804638c85e4f960d59a574094e172e5 |

`inference.yml` (the dictionary; task 4 pins these too), SHA-256 as downloaded on 2026-11-20 (these files are not LFS, so the hub shows no hash; recheck when writing the manifest):

- det: 98069072e1b6b37d727fd9d9f11725faa46d6ea0de012f2ed26caea011c37699
- rec (zh, en, ja): 5dfeb2777f6d0db8177d8128a8acfcf6e6276dc4ac73ea3bf0dc06d6a5e85d8e
- latin: 0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84
- eslav: 025039bac23eb4a308efcefa4d58eab3af440767815c6ba6938468bf6353ee5a
- cyrillic: 5c76cc91fa98410178a09f498db10050d0ec1634a660053d3005ab7be581f501

 There is no Hebrew recognizer in the PaddlePaddle organisation (checked the full list: `PP-OCRv5` server and mobile, `latin`, `eslav`, `cyrillic`, `en`, `korean`, `el`, `th`, `te`, `ta`, `arabic`, `devanagari`).

Accuracy (CER %, best recognizer for the script; the whole table is in the run output of `bench`):

| Fixture | Best recognizer | CER % | Wrong recognizer (example) |
|---|---|---|---|
| en-clean | any | 0.0 | |
| de-clean, de-blur | latin | 0.0 | zh/en model 0.8 and 3.5 |
| ru-clean | eslav or cyrillic | 0.0 | latin 95, zh/en model 96 |
| ru-noise | eslav or cyrillic | 0.0 | zh/en model 97 |
| zh-clean, zh-noise, ja-clean | zh/en/ja | 0.0 | latin and Cyrillic models 100 |
| en-skew | any (after oriented boxes) | 0.0 | |
| mixed-clean (en + ru) | eslav | 1.6 | cyrillic 3.1, latin 64 |
| he-clean | none | 97.7 to 100 | all five |

`eslav` and `cyrillic` read the Russian fixtures equally well; `eslav` is slightly smaller. Cyrillic scripts outside East Slavic (Bulgarian, Serbian, Kazakh...) are the reason to ship `cyrillic` later; for v0.1.0 `eslav` is enough.

Time per page (warm sessions, detection plus recognition of 2 to 3 lines): about 70 to 150 ms. Detection model 4.8 MB, each recognizer 8 to 16.5 MB.

Pipeline findings that task 5 must take over:

- Detection boxes must be **oriented**. The first spike version used axis-aligned bounding boxes of each connected component and read `en-skew` at 70 % CER (tall boxes with two lines' worth of slanted text). The final version takes the principal axis of each component (PCA), makes a rotated rectangle, unclips it (ratio 1.5), and cuts the line out with a rotation: `en-skew` 0.0 %. It is ~100 lines in `PaddleOcr.kt` and uses only the JDK; a Kotlin multiplatform version needs its own pixel loops (no `BufferedImage` on Android).
- Detector input: longest side limited to 960, sides rounded to multiples of 32; mean/std normalisation applied in BGR order as in the reference code; threshold 0.3, box score 0.6.
- Recognizer input: height 48, width proportional (max 3200), `(x/255 - 0.5)/0.5`; output `[1, T, classes]`; CTC decoding with index 0 as blank, dictionary from the yml, space appended as the last class.
- ONNX Runtime warns about unused initializers in these models at load; set the session log level to ERROR.

## Language not given: strategies compared

All on the five installed recognizers, CER % (time):

| Strategy | Result |
|---|---|
| A: run the zh/en/ja model, read the script of its output, re-run with the matching recognizer | **Fails for Cyrillic**: the default model reads Russian as junk (94 to 99 % CER), so the script of its output is wrong. Rejected. |
| B: run every installed recognizer, keep the highest mean confidence | Correct on every non-Hebrew fixture, 320 to 630 ms. |
| C: detect once, run every recognizer on the two longest lines only, then read all lines with the winner | **Same results as B**, 220 to 450 ms; detection is shared. Chosen. |

Confidence is the mean of the winning class probability over the kept characters. Caveat: confidence is high on junk too (a Latin recognizer reads Hebrew with 0.84), so it only ranks the installed recognizers against each other; it does not prove that any of them is right. Hebrew is handled before this step (below).

## Hebrew

PP-OCRv5 cannot read it (97.7 to 100 % CER with every recognizer). Tesseract `heb` reads the fixture at **0.0 % CER**, both `tessdata_fast` (heb 0.96 MB) and `tessdata_best` (heb 3.7 MB). Deciding Hebrew from the image when the language is not given: Tesseract `heb` mean word confidence is 93 on the Hebrew fixture and 18 to 51 on every other fixture (en 43, de 33, ru 30, zh 19, ja 41, mixed 51, skew 39, noise 35, blur 27), so a threshold near 80 separates them in this set, at about 100 ms. Mixed Hebrew + Latin text was not tested.

Decision for task 7: **Hebrew goes through Tesseract; the task is needed.** If Hebrew is installed and no language was given, run the `heb` probe first; confidence of 80 or more means Hebrew, otherwise go on with strategy C.

## Tesseract comparison (all six languages)

Both data sets read every fixture at 0.0 % CER except ja-clean 2.0 % (one character); mixed-clean was not run. Time per page: `tessdata_fast` 40 to 90 ms through JavaCPP and 80 to 260 ms through tess4j (first call 700 ms); `tessdata_best` 240 to 1300 ms through tess4j.

| Data | eng | deu | rus | heb | chi_sim | jpn |
|---|---|---|---|---|---|---|
| tessdata_fast | 4.1 MB | 1.5 MB | 3.9 MB | 0.96 MB | 2.5 MB | 2.5 MB |
| tessdata_best | 15.4 MB | 8.6 MB | 15.3 MB | 3.7 MB | 13.1 MB | 14.3 MB |

(Licenses: both repositories Apache 2.0 on GitHub.) On these clean, rendered pages Tesseract is as accurate as PP-OCRv5, so this set does not show PP-OCRv5's advantage on real photos and perspective; the choice of PP-OCRv5 as the main engine rests on its published photo accuracy and on one pipeline for four scripts, not on these numbers. If M2's hand test shows PP-OCRv5 failing on real photos, Tesseract for all languages is a fallback that costs no new engine, since it is needed for Hebrew anyway.

Which Tesseract wrapper: **JavaCPP presets** (`org.bytedeco:tesseract:5.5.3-1.5.14`, with `leptonica:1.87.0-1.5.14` and `javacpp:1.5.14`). It ran on the JVM here and has native jars for `windows-x86_64` (2.1 MB), `linux-x86_64` (2.0 MB), `android-arm64` and `android-x86_64` (1.96 MB for arm64): one wrapper and one code path for JVM and Android. `tess4j` 5.21.0 (the alternative named in 0005) bundles only a Windows DLL (`win32-x86-64/libtesseract554.dll`), no Linux library, so it would need a system Tesseract on Linux: rejected. `tesseract4android` was not found on Maven Central. JavaCPP unpacks its natives to `~/.javacpp/cache` (observed: `jnijavacpp.dll`, `libtesseract*.dll`, `leptonica` libraries and MSVC runtime DLLs, tens of files) and not to the temp folder; the documented system property `org.bytedeco.javacpp.cachedir` should move it into the data folder; not tested, task 3 verifies. Android did not run: the JavaCPP path on the emulator is a task 3 and task 7 item.

## ONNX Runtime

- One Maven artifact for the JVM covers all desktop OSs: `com.microsoft.onnxruntime:onnxruntime:1.32.0` (MIT), a 55.7 MB jar with natives for `win-x64` (onnxruntime.dll 20.6 MB, jni 0.1 MB, providers_shared 22 KB), `linux-x64` (34.8 MB), `linux-aarch64`, `osx-aarch64`. The jar contains macOS debug symbols (58 MB) that must not go into the app image; the app needs only the target OS libraries (jlink/jpackage input to be trimmed in task 3). Android: `onnxruntime-android:1.32.0` AAR is 59.3 MB (all ABIs; limit with ABI splits).
- **Native library location.** ORT unpacks its three DLLs (20.7 MB) into `<java.io.tmpdir>/onnxruntime-java<random>` on every start and does **not** delete them on exit on Windows (4 folders from 4 runs found). `-Donnxruntime.native.path=<dir>` makes it load `onnxruntime.dll` and the jni library from `<dir>`, but it still extracts `onnxruntime_providers_shared.dll` into temp, so that property alone does not keep the app out of the temp folder. **`-Djava.io.tmpdir=<data>/tmp`** moves everything into the data folder (verified: all three DLLs under it, nothing in `C:\Temp`). Plan for task 3: `main` sets `java.io.tmpdir` to `<data>/tmp` before any other code touches the temp folder, and removes stale `onnxruntime-java*` folders at startup. The JVM's own `hsperfdata` (written before `main`) is removed with `-XX:-UsePerfData` in the packaged app's JVM options.

## Language-detection libraries (maintenance only; accuracy is task 6)

- Optimaize `com.optimaize.languagedetector:language-detector` 0.6: last release **2016-10-07**. Unmaintained.
- Lingua `com.github.pemistahl:lingua` 1.2.2 (Apache 2.0): last release 2022-08, jar 80 MB with all languages; needs restricting to our language list. Tiny Lingua is not on Maven Central. Neither was run in this spike.
- Task 6 starts from script detection by Unicode block (`Scripts` in the spike, 20 lines) plus Lingua restricted to our languages, and measures it on OCR output; if its size is a problem, a small own n-gram detector for the languages in the picker is the alternative.

## Not measured

- Linux (CI only), Android and any phone CPU: the spike ran on the Windows JVM. ONNX Runtime on the emulator and JavaCPP Tesseract on Android are task 3 and task 7.
- Real photographs and handwriting; vertical text; mixed Hebrew and Latin; memory use.
- Whether `org.bytedeco.javacpp.cachedir` really redirects the JavaCPP cache.

## Result

Recommendation to the owner (0005 updated accordingly): accept PP-OCRv5 on ONNX Runtime for Latin, Cyrillic (`eslav`), Chinese and Japanese, Tesseract through JavaCPP for Hebrew, strategy C for "no language given" with a Tesseract `heb` probe first when Hebrew is installed.
