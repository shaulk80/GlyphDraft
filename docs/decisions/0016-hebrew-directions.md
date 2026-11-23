# 0016: Hebrew directions

Date: 2026-11-23. Status: proposed (M3 task 6; the owner accepts by merging the pull request). Follows 0012 (engine), 0014 (manifest and routes) and 0015 (translator). The owner read the Hebrew outputs and decided the mirror and the own export on 2026-11-23 (points 3, 4 and 6).

- **Problem.** The spike found working Hebrew models only for `he to en` and `ru to he`, and no ONNX model at all from OPUS-MT for the Xenova-style repositories. Task 6 was to test the other exports of the same uploader (`R4kSo1997`), fix or replace the broken ones, and decide license, mirror and the experimental mark.

## What was tested

Seven int8 exports of the uploader, 144 to 150 MB each, pinned at their current commit and run through the whole translator (`FixtureTranslationTest`, the 30 reference sentences, chrF):

| Direction | chrF | Result |
|---|---|---|
| he to de | 71.6 | works |
| en to he | 3.1 | broken |
| he to ru | 0.4 | broken |
| de to he | 8.8 | broken |
| fr to he | 3.3 | broken |
| he to es | 3.2 | broken |
| es to he | 0.1 | broken |

"Broken" means fluent text that has nothing to do with the source (`- ¡Si! - ¡Si!`, or one word repeated until the length limit): the decoder runs but the encoder's answer is not used, which points at a damaged int8 export, not at our loop. The loop is the same one that gives `he to en` 78.9, `ru to he` 68.2 and `he to de` 71.6 through the same plain decoder, and the tokenizer was ruled out in the spike (en to he with the pieces of `tokenizer.json` and of `source.spm`). Which export step damaged the six models was not found (no fp32 files to compare with, and Python with PyTorch is not installed here). There is no `he-fr` export.

## Decision

1. **No new direction is added. The 13 directions of 0014 stay.** The only working new export, `he to de`, is worse than the route that exists already: `he to de` through English (`he to en` 78.9, `en to de` 81.5 for the steps) scores **75.6**, the direct export 71.6, and it would cost another 144 MB download and one more unlicensed file on a repository we do not control.
2. **Every Hebrew pair is served by the routes of 0014**, measured now through the whole translator and kept as regression cases in `FixtureTranslationTest` (they run when the models are installed; the floor is 5 points under the figure):

| Pair | Route | chrF |
|---|---|---|
| he to en | he, en | 78.9 |
| he to de | he, en, de | 75.6 |
| he to es | he, en, es | 76.9 |
| he to ru | he, en, ru | 64.6 |
| en to he | en, ru, he | 67.3 |
| ru to he | ru, he | 68.2 |
| de to he | de, en, ru, he | 66.0 |
| es to he | es, en, ru, he | 62.3 |
| fr to he | fr, en, ru, he | 64.2 |

   A Hebrew target costs two to three models (en to ru 111 MB and ru to he 149 MB, plus the source's), a Hebrew source one to two.
3. **Experimental stays** on `he to en` and `ru to he` (0014), and so a pair is experimental when any pack of its route is; the UI of task 9 derives that from `packsForRoute`, not from a second list. **Read by the owner (native reader) on 2026-11-23** (`docs/manual/m3-hebrew-read.md`, 90 outputs): 66 of 90 plain OK, most of the rest OK with a more precise reference or a stylistic fault, one wrong meaning (en to he, "I am sorry, but I do not understand what you mean" came out as "I don't know what you are talking about"), no offensive text, nothing invented out of the blue but meaning that shifts slightly. Verdict: good enough for everyday and hobby use, not for sensitive documents. That is what "experimental" says, so the mark stays and the UI text should say it. One concrete defect: the Hebrew-target models write times as "9: 00" with a space (and once the wrong hour, "2: 30" for half past three); issue #45.
4. **Own export of `en to he` and `he to ru`: not done (owner, 2026-11-23).** The route through Russian is good enough for the current needs, so no Python or Optimum is needed and no new dependency. If it is wanted later: `optimum-cli export onnx --model Helsinki-NLP/opus-mt-en-he --task text2text-generation-with-past`, then int8 quantization as the Xenova repositories were made (the exact flags are not verified), then `FixtureTranslationTest` before anything else.
5. **License.** The base models are Helsinki-NLP releases: `opus-mt-tc-big-he-en` is CC-BY 4.0 and `opus-mt-ru-he` Apache 2.0 on their cards (checked on the hub on 2026-11-23). The uploader's two repositories we ship (`he-en`, `ru-he`) have no license text and no base-model tag; they are int8 conversions of those models, so the base licenses apply and the credit line of task 10 names Helsinki-NLP, OPUS-MT and the uploader of the conversion. This is the reading of one non-lawyer; if it matters for a public launch, ask.
6. **Mirror: none (owner, 2026-11-23).** Both Hebrew models are pinned by commit and checksum, so a changed file is refused; a removed repository would break first installs of `he to en` and `ru to he` (370 MB and 149 MB). The owner accepts that risk for now; online (cloud) models at a later stage are the backup plan.
7. **`he to en` (366 MB) stays for now.** Whether it stays in the app depends on its memory on Android (task 7), as planned.

## Alternatives rejected

- Shipping `he to de` as a direct direction: worse than the pivot and one more file we do not control.
- Taking other community exports (`Torick63/opus-mt-en-he-onnx-quantized`): a different graph (fixed-size encoder input), not run in the spike, no license, no better provenance.
- Larger multilingual models (NLLB, M2M100, MADLAD) for Hebrew: 750 MB and more (0012).
