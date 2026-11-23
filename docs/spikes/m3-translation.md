# M3 task 1: translation engine spike

Date: 2026-11-23. Code: `spikes/translation` (stand-alone Gradle build, not part of the product build; run `.\gradlew -p spikes/translation run --args="bench en-de de-en"` after downloading the models into `data/spike-models/mt/<pair>/`). Reference set: `engine/src/test/resources/translation` (30 sentences, 8 languages). Everything ran on the JVM on Windows 10 (x64, 4 ORT threads, greedy decoding); Linux, Android and phone speed are not measured (see "Not measured"). About 45 tool calls.

## Reference set

30 everyday sentences (notices, requests, short news lines), written for this project in English and translated by hand into de, fr, es, ru, zh, ja, he, one file per language, line N is the same sentence in every file. So the references for en to X are the X file, and for X to en the sources are the X file and the reference is `en.txt`. The hand translations are mine and have not been checked by a native speaker; chrF between a good machine translation and a single reference is lower than the quality of the machine translation. Limit: short, clean, one sentence per line. Real OCR text has errors, line breaks and longer sentences, so real scores will be lower.

Metric: corpus chrF (own code in `Metrics.kt`: character n-grams 1 to 6, whitespace ignored, beta 2), 0 to 100. BLEU was not computed (chrF is the better measure for the CJK and Hebrew targets). chrF on Chinese or Japanese is not comparable to chrF on alphabetic languages (characters are whole words, so n-grams match far less often), so for zh and ja targets the outputs were also read.

## Candidates

| Candidate | Result |
|---|---|
| (a) **OPUS-MT (Marian), ONNX int8 on ONNX Runtime** | **Works.** `Xenova/opus-mt-*` repos (Optimum export, `encoder_model_quantized.onnx` plus `decoder_model_merged_quantized.onnx` with key/value cache) run with the ORT jar the app already ships. Own greedy decode loop, 150 lines; own SentencePiece unigram tokenizer in Kotlin, 90 lines, no native library (reads `tokenizer.json` for pieces and scores and `vocab.json` for the Marian ids; `source.spm` is not needed). Chosen. |
| (b) Bergamot / Firefox Translations | Not run. Models are Marian binaries for the native bergamot-translator, which needs a C++ build for Windows, Linux and Android; no ONNX form. A new native library, so (a) wins unless (a) fails; it did not. |
| (c) CTranslate2 | Not run. No official JVM or Android binding found. |
| (d) M2M100 / NLLB / MADLAD | Not run, size: `m2m100_418M` ONNX is 757 MB (fp32 files), NLLB-600M and MADLAD larger. Far over the ceiling. |
| ML Kit Translate | Rejected on sight (Android only, proprietary). NLLB-200 weights are believed to be CC-BY-NC (non-commercial; not re-checked), and large anyway. |

## Results (OPUS-MT int8, greedy, 30 sentences)

| Direction | chrF | ms per sentence | Files (MB) | Verdict |
|---|---|---|---|---|
| en to de | 81.5 | 204 | 106 | good |
| de to en | 85.9 | 199 | 106 | good |
| en to fr | 82.4 | 242 | 107 | good |
| fr to en | 79.0 | 215 | 107 | good |
| en to es | 83.4 | 236 | 113 | good |
| es to en | 81.9 | 234 | 113 | good |
| en to ru | 70.3 | 230 | 111 | good |
| ru to en | 72.6 | 221 | 111 | good |
| en to zh | 37.1 (not comparable) | 185 | 113 | readable; ASCII punctuation, final stop often missing, one wrong meaning in 8 read; experimental |
| zh to en | 72.9 | 231 | 113 | good |
| ja to en | 72.3 | 215 | 109 | good |
| **en to ja** | **8.3** | 292 | 94 | **unusable**: the model (`opus-mt-en-jap`) answers in archaic, space-separated Bible-style Japanese unrelated to the input |
| he to en | 82.5 | 147 | **366** | good, but this is the "tc-big" model (see below) |
| **en to he** | **3.1** | 261 | 148 | **no working model**: the only ONNX export found repeats one word (below) |

Time is wall time per sentence on the Windows desktop CPU, 4 threads, after one warm-up sentence, about 55 to 63 generated tokens per second. Load of one direction: 0.6 to 1.5 s. Process working set after loading and running one direction: 510 to 620 MB (JVM plus ORT arena plus the 106 MB model; measured in separate steps in one process it grew to about 1 GB after eight directions even though sessions were closed, so the app must keep at most one direction open).

Pivot through English (source to en to target, two greedy runs, both models loaded):

| Pair | chrF | ms per sentence |
|---|---|---|
| de to fr | 78.4 | 439 |
| fr to es | 77.3 | 449 |
| ru to de | 74.6 | 437 |
| es to ru | 67.8 | 460 |
| zh to fr | 68.3 | 466 |
| ja to de | 67.7 | 436 |
| de to zh | 36.2 (as en to zh) | 391 |
| ru to zh | 29.9 (as en to zh) | 404 |

Pivoting costs 3 to 8 chrF points against the direct step and twice the time; usable. The language-pair table for the five non-English languages that work as targets (de, fr, es, ru, zh) and the sources (de, fr, es, ru, zh, ja, he) is therefore complete through English.

## Size against the ceiling

The working ceiling is 150 MB per pack of both directions. One int8 direction is **106 to 113 MB** (encoder about 50 MB, decoder about 57 MB, each carrying its own copy of the 58k-token embedding). A pack of both directions is **213 to 226 MB**, over the ceiling by about 50 percent. The other variants on the hub are bigger (`q4` 269 MB, `q4f16` 146 MB per direction, `fp16` 209 MB); int8 is the smallest. Sharing the embedding between encoder and decoder, or pruning the vocabulary to the languages we need, would save about 30 to 40 MB per direction but needs a re-export (Python, Optimum, PyTorch are not installed here).

Total if every working direction is installed: 11 directions, about 1.2 GB. Hebrew to English alone is 366 MB (`opus-mt-tc-big-he-en`, 1024-dimension model), the base `opus-mt-he-en` does not exist.

## Hebrew and Japanese as targets

- **Hebrew to English** works (chrF 82.5) with `R4kSo1997/opus-mt-he-en-onnx-int8` (a private, unofficial export of `Helsinki-NLP/opus-mt-tc-big-he-en`, CC-BY 4.0), 366 MB, a plain decoder without cache (my loop re-reads the prefix each step; 147 ms per sentence anyway).
- **English to Hebrew**: the only ONNX models found are the private exports `R4kSo1997/opus-mt-en-he-onnx-int8` (148 MB) and `Torick63/opus-mt-en-he-onnx-quantized`. The first loads and runs but repeats one word for every sentence (chrF 3.1) with either the `tokenizer.json` pieces or the pieces read from `source.spm`, so the tokenizer is not the cause; the decode loop is the same as for the working he-to-en model, so a faulty int8 export is the likely cause (not verified). The second has a different graph (fixed-size encoder input, `decoder_input_ids`) and was not run. To get a Hebrew target one must export `Helsinki-NLP/opus-mt-en-he` oneself (Python with Optimum, int8), which needs tools that are not installed here.
- **Hebrew and Russian directly** (added later the same day): `Helsinki-NLP/opus-mt-he-ru` and `opus-mt-ru-he` exist (Apache 2.0 on the card, base size) and the same private uploader has int8 exports of both (`R4kSo1997/opus-mt-{he-ru,ru-he}-onnx-int8`, 144 MB each: encoder 52 MB, plain decoder 92 MB). **Russian to Hebrew works: chrF 68.2**, 60 ms per sentence. **Hebrew to Russian is broken** the same way as en-he (chrF 0.4, degenerate output), so a faulty int8 export again (not verified); it could be exported again by us or reached as he to en to ru. Hebrew as a target through Russian: **en to ru to he: chrF 67.3**, 282 ms per sentence, two models (111 + 144 MB). The same uploader has `de-he`, `es-he`, `fr-he`, `he-de`, `he-es`, `he-fr` and more; none was tested.
- **English to Japanese**: `Helsinki-NLP/opus-mt-en-jap` is the only OPUS-MT model for the direction and it is trained on a small religious corpus; it cannot translate everyday text. No other small model exists; M2M100, NLLB and MADLAD are 750 MB and more. Japanese works as a source only.

## Licenses

The base models are Helsinki-NLP releases from the OPUS-MT project; the hub cards say `cc-by-4.0` for `en-de`, `ru-en`, `zh-en`, `tc-big-he-en` and `apache-2.0` for the rest, and the README of `opus-mt-en-de` says CC-BY-4.0. Treat all as **CC-BY 4.0: free use, attribution required** (a credits line in the app and README naming Helsinki-NLP, OPUS-MT and the Xenova conversion). The `Xenova/*` ONNX repos carry no license field of their own (derived from the base model). Code: ONNX Runtime MIT (already in), no new dependency. The allow-list in `config/` is for code dependencies, so it does not change; models are pinned by URL, SHA-256 and size as in decision 0007. The community Hebrew exports have no license text at all.

## Android

Nothing new to ship: ORT for Android is already in the app and OPUS-MT int8 uses standard ONNX operators (`MatMulInteger`, `DynamicQuantizeLinear`); the tokenizer is plain Kotlin. Not run on the emulator in this task (task 6). Risks to check there: peak memory (about 500 MB of working set on the desktop for one direction), speed on a phone CPU (expect 3 to 5 times the desktop time, so a ten-sentence paragraph would take 10 to 15 s), and the 106 MB file in app storage.

## Not measured

- Beam search (the models default to 4 beams): the figures above are greedy; beam usually adds 1 to 2 chrF but needs a batched cache. Left to task 5 as an option.
- Android, Linux, a real phone; long paragraphs; noisy OCR text as input; sentences over about 100 tokens; text in several languages in one block.
- Whether the Marian source normalisation (NFKC style) matters for full-width Chinese or Japanese punctuation in the source: the zh and ja sources scored 72 to 73 without it.
- Checksums: the SHA-256 values are recorded in task 4 when the manifest is written (the `Xenova` repos are not LFS-pinned by revision here; pin a commit hash).

## Result

See decision 0012 (proposed). In short: OPUS-MT int8 on ONNX Runtime with our own Kotlin tokenizer works and is good for de, fr, es, ru, zh and ja and he as sources, English as the pivot costs 3 to 8 chrF; but the pack is 213 to 226 MB (over the ceiling), Hebrew as a target and Japanese as a target have no usable model.

## Addendum (task 6, 2026-11-23)

The other Hebrew exports of the uploader were run through the real translator: en-he, he-ru, de-he, es-he, fr-he and he-es are broken (chrF 0.1 to 8.8), he-de works (71.6) but is worse than he to en to de (75.6). The he-ru and en-he failures of this spike were therefore not a tokenizer or end-id mistake of the spike. Details, the pivot figures of every Hebrew pair, license and mirror: decision 0016.
