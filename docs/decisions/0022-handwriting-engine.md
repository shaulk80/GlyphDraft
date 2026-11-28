# 0022: Handwriting recognition for M4

Date: 2026-11-27. Status: **proposed**. The owner's answers (below) are accepted; the reader per language and the cloud provider are decided after the spikes (M4 tasks 2 and 3).

- **Problem.** M4 needs handwritten text read behind `OcrEngine`, on-device first. Where the on-device quality is poor, the milestone records that and adds an optional cloud engine (`docs/plan.md`).

## Owner's answers (2026-11-27)

1. All eight languages. The owner writes samples for English, Hebrew and Russian; a free AI model draws them for German, French, Spanish, Chinese and Japanese.
2. An online reader is acceptable, given recommendations (below).
3. Quality bar, mean CER (character error rate) per language on the fixtures: up to 10 % good; above 10 and up to 25 % shipped marked experimental; above 25 % poor. The figures are a choice, not derived from a measurement.
4. TrOCR may be used (weights tagged MIT, fine-tuned on IAM, a dataset offered for non-commercial research).
5. #57 before the release; #51 and #54 after M4 unless they block handwriting.
6. A language measured only on AI-drawn samples ships as experimental at best. Because of that, an AI agent told to do exactly this may proofread them.
7. Claude testing is to start on the owner's Claude Pro subscription (see below).
8. The cloud spike also tests whether a model can correct the writer's obvious mistakes, not only read them.
9. The app offers that correction as an optional switch.

## Samples

- AI-drawn samples are probably more regular than real handwriting, and image models can misdraw text. So their figures are reported separately as synthetic, and the cloud provider is chosen on the real samples only (a model may read drawings of its own family better).
- Proofreading AI-drawn samples: the agent gets each image and the intended text, reports every place where they differ, and the expected text follows the image. The changes are listed in the fixtures README.

## Testing Claude with a Pro subscription

- The subscription does not include the API, which is billed separately ([help page](https://support.claude.com/en/articles/9876003-i-have-a-paid-claude-subscription-pro-max-team-or-enterprise-plans-why-do-i-have-to-pay-separately-to-use-the-claude-api-and-console)). Claude Code's `claude -p`, signed in with the plan, draws from the subscription's usage limits ([help page](https://support.claude.com/en/articles/15036540-use-the-claude-agent-sdk-with-your-claude-plan), read 2026-11-27).
- The terms describe those limits as for ordinary, individual use; they do not name personal scripts. A spike over a few dozen images on the owner's machine is taken to be that.
- The app cannot use the subscription: third-party applications must use API keys and may not offer a claude.ai login or use plan credentials ([terms](https://code.claude.com/docs/en/legal-and-compliance)). A Claude reader in the app needs a paid Console key.
- Intended run, **not tried yet**: from a folder outside the repo that holds only the images under neutral names (no expected text), one new session per image, `claude -p --safe-mode --tools Read --no-session-persistence --output-format json --model <model>`. `--bare` cannot be used: it ignores the subscription login. `ANTHROPIC_API_KEY` must be unset, or the key is billed.
- The CER from this route is an indication only: Claude Code adds its own instructions and hands the image over through its Read tool, so the API may read differently. Its time and cost figures do not carry over.
- Whether the plan's data is used for training follows the setting at claude.ai/settings/data-privacy-controls (kept 5 years if allowed, else 30 days; [data usage](https://code.claude.com/docs/en/data-usage)).

## On-device candidates

Sizes and license tags from the Hugging Face model pages on 2026-11-27. Nothing here was run on our own images.

| Candidate | Handwriting for | Download | License tag | Cost and doubts |
|---|---|---|---|---|
| **PP-OCRv6 small recognizer** (`PaddlePaddle/PP-OCRv6_small_rec_onnx`) | en, zh; its dictionary has German, French and Spanish letters (ja: only printed figures seen), no Cyrillic or Hebrew | 21 MB | Apache 2.0 | Cheapest: looks like the same CTC pipeline (same input description; output not checked) |
| PP-OCRv6 medium recognizer | as above | 77 MB | Apache 2.0 | Slower |
| PP-OCRv5 server recognizer | zh, en, ja | 85 MB | Apache 2.0 | Behind v6 small in the vendor figures below, except handwritten Chinese (0.4 points ahead) |
| **TrOCR small handwritten**, int8 (`Xenova/trocr-small-handwritten`) | en only | about 68 MB (encoder, merged decoder, tokenizer) | MIT on the Xenova page, none on Microsoft's small model (MIT on base) | New pipeline (one line in, ids out) |
| TrOCR base handwritten, int8 | en only | about 340 MB | as above | Memory on a phone |
| `raxtemur/trocr-base-ru` | ru (card: CER 6.6 to 9.3 % on HKR and Cyrillic sets) | about 340 MB if exported to int8 | Apache 2.0 | No ONNX files: our own export and hosting |
| `cyrillic-trocr/trocr-handwritten-cyrillic` | ru, uk, Church Slavonic (card: validation CER 25 %) | as above | MIT | As above |
| `qantev/trocr-base-spanish`; `agomberto/trocr-large-handwritten-fr` | es (trained on handwriting: not checked); fr | if exported to int8: es about 340 MB, fr much more | MIT | Own export and hosting; the French one is far over the size ceiling of decision 0012. Only if PP-OCRv6 is poor |

Vendor figures (PaddleOCR model cards, their own benchmark; share of lines read with no error, so these are not CER):

| Model | Handwritten EN | Handwritten CN | Printed EN | Printed CN |
|---|---|---|---|---|
| PP-OCRv5 mobile zh/en/ja (shipped) | 50.9 | 41.7 | 86.0 | 86.0 |
| PP-OCRv5 server | 59.6 | 58.0 | 85.1 | 90.1 |
| PP-OCRv6 small | 61.1 | 57.6 | 93.3 | 90.5 |
| PP-OCRv6 medium | 67.8 | 62.1 | 94.1 | 91.5 |

The shipped `latin` recognizer, which reads German, French, Spanish and usually English for us, is not in that table.

### Not usable on-device

- **Hebrew:** the one recent handwriting model found, `itayinbar/Mishkefet-v1`, is CC BY-NC-SA 4.0 (non-commercial). Tesseract `heb` is a printed-text model; task 1 measures it on handwriting, with little hope.
- **German:** the fine-tune looked at (`fhswf/TrOCR_german_handwritten`) is large (2.2 GB unquantized) and AFL-3.0, not on the allow-list.
- **Vision-language OCR models** (GLM-OCR, MIT, 2.7 GB; PaddleOCR-VL, Apache 2.0, 1.9 GB; LightOnOCR-2-1B, Apache 2.0, 2.0 GB, full precision): far above the size ceiling and likely to need another runtime.

## Cloud candidates

### Dedicated OCR services: not recommended

- **Google Cloud Vision:** its handwriting script table has no Hebrew; Latin and Japanese are "supported", Cyrillic and Chinese "experimental" ([languages](https://docs.cloud.google.com/vision/docs/languages)).
- **Azure Read:** handwriting in nine languages, without Russian or Hebrew ([language support](https://learn.microsoft.com/en-us/azure/ai-services/computer-vision/language-support)).
- **Mistral OCR:** no evidence on Hebrew handwriting found; not looked into further.

### Vision language models

Hebrew handwriting, [ivrit.ai leaderboard](https://huggingface.co/spaces/ivrit-ai/hebrew-handwriting-ocr-leaderboard) generated 2026-09-04 (10 pages, 225 lines, 10 writers). CER in %:

| Model in the benchmark | Line, median | Line, all text | Full page |
|---|---|---|---|
| gemini-flash (13 blank answers left out of the median) | 12 | 219 | 76 |
| gemini-flash-lite | 28 | 33 | 35 |
| gpt-5.6-sol | 44 | 42 | 40 |
| gpt-5.6-terra | 58 | 58 | 60 |
| claude-sonnet-5 | 62 | 60 | 62 |
| claude-opus-5 | 69 | 96 | 53 |
| gpt-5.6-luna | 73 | 73 | 130 |
| claude-haiku-4-5 | 90 | 91 | 79 |

- `gemini-pro` is left out of the table: most of its answers were blank.
- No model reaches "good", and on "all text" or full page none reaches "experimental". A figure over 100 means the output was mostly or partly invented text. Hebrew handwriting may be poor even online.
- The Hugging Face names above are aliases or older versions; the current models below are not in the benchmark.
- For Russian cursive no comparison of current models was found. A mixed-language benchmark ([METATR](https://arxiv.org/html/2605.26712)) put Gemini 3 Pro first (CER 28.5 %), Claude Opus 4.5 second (31.2 %), GPT-5.1 far behind (59.5 %) on handwriting.

| Provider, models to spike | USD per 1M tokens, in / out | Data use (provider's pages, 2026-11-27) |
|---|---|---|
| **Google Gemini API**: `gemini-3.5-flash-lite`; `gemini-3.8-flash`; `gemini-4-hydrogen` | 0.30 / 2.50; 0.75 / 3.75 (double from 2027-01-01); 1.50 / 7.50; all three have a free tier | Free tier: content is used to improve products and may be read by reviewers. Paid: not used. API clients offered to users in the EEA, Switzerland or the UK must use paid services; users 18 or older ([pricing](https://aistudio.google.com/docs/pricing), [terms](https://aistudio.google.com/docs/terms)) |
| **OpenAI**: `gpt-6-luna`; `gpt-6.2-sol` | 0.10 / 0.50; 2 / 10 | Not used for training unless opted in; abuse logs up to 30 days ([pricing](https://developers.openai.com/api/docs/pricing), [data](https://developers.openai.com/api/docs/guides/your-data)) |
| **Anthropic Claude API**: `claude-haiku-5-5`; `claude-opus-5-5` | 0.10 / 0.50 (prompts to 100K tokens); 4 / 20. A large photo is up to 4784 input tokens | Retained data is not used for training without permission; content not retained by default for these models, flagged content up to 2 years ([retention](https://platform.claude.com/docs/en/manage-claude/api-and-data-retention), [vision](https://platform.claude.com/docs/en/build-with-claude/vision)) |

### Recommendation

1. **First provider: Gemini.** Best Hebrew figures found, a free tier to try it, one API key. Tell the user the free tier's data use before the first upload.
2. **Second: OpenAI**, second to Gemini Flash-Lite on full pages, no training by default.
3. **Claude** is worth one spike run, since the current models are unmeasured, but the older ones were weak on Hebrew. (This record was drafted with a Claude model.)
4. Build one provider in M4; the `OcrEngine` boundary keeps a second one cheap to add.

The spike starts with Claude because the owner can test it without a new key; that does not change the ranking, and Gemini's free tier should be measured before the provider is chosen.

## First measurements (2026-11-27)

One photographed page of Hebrew cursive (fixture `he-school`, a school assignment; the owner consents to it as a public fixture), one run per row, free tier, photo sent without metadata. CER against the page's text as written, **verified by the owner** on 2026-11-27. Blank length and quote and dash shapes were not counted. The scripts are not in the repository.

| Model, thinking level | Asked for | CER % | Words found % | Seconds | Paid-tier USD |
|---|---|---|---|---|---|
| gemini-4-hydrogen, high (default) | as written | 2.0 | 94.3 | 144 | 0.25 |
| gemini-4-hydrogen, medium | as written | 1.3 | 94.9 | 24 | 0.025 |
| gemini-4-hydrogen, low | as written | 1.7 | 93.8 | 8 | 0.012 |
| gemini-4-hydrogen, medium | as written and corrected | 1.6 | 93.8 | 22 | 0.034 |
| gemini-4-hydrogen, low | as written and corrected | 1.4 | 94.3 | 11 | 0.016 |
| gemini-3.5-flash-lite | as written | 16.7 | 75.6 | 5 | 0.001 |
| gemini-3.8-flash | as written | no answer | | | |
| an earlier Claude transcript (model and settings not recorded) | corrected | 20.9 | 86.4 | | |

- The reference was drafted by a Claude model before any Gemini output was seen (that draft scores CER 0.8 %), then corrected by the owner in five places: three where the draft had fixed the writer's mistake, one dropped letter, one misread word.
- That misread word was wrong in every reading in the table and in the draft; only the owner's check found it. Agreement between readers does not prove a reading, and an AI proofreader (answer 6) can share such a mistake.
- CER and words for the "as written and corrected" rows score the as-written part.
- Thinking level (`generationConfig.thinkingConfig.thinkingLevel`; Hydrogen cannot turn thinking off) changed time and cost far more than accuracy.
- Hydrogen's "as written" text is not literal: in every run it completed a word the writer left a letter out of.
- Asked for both texts, it kept every sentence in order and the changes can be listed by comparing the two. At low, one change replaced a word with a different one.
- The Claude transcript fixed real mistakes but also dropped part of a sentence, reordered sentences and replaced words with ones of another meaning.
- Availability: `gemini-3.8-flash` never answered (HTTP 503 or time-out); Hydrogen needed a second run once and a retry twice.
- The shipped Hebrew model (Tesseract `heb`, a printed-text model) on the same page: CER 83.0 % on the 3072 x 4080 original, 75.9 % on the 2048-pixel fixture made from it (`HandwritingBaselineTest`, which also folds full-width forms and more quote shapes). The rows above were read from the original, so later runs on fixtures are not directly comparable with them.
## Baseline of the shipped models (2026-11-28)

`HandwritingBaselineTest` over the 41 fixtures, each page read in its own language, on Windows. The owner verified the handwritten texts (2026-11-28); the AI-drawn ones were proofread by an AI agent. Page figures are in the report that `scripts/build.ps1` prints.

| Language | Samples | Pages | Mean CER % | Worst page CER % | Against the bar (answer 3) |
|---|---|---|---|---|---|
| English | real | 5 | 3.1 | 9.5 | good |
| Russian | real | 5 | 20.9 | 33.9 (`ru-05`) | experimental |
| Hebrew | real | 6 | 64.1 | 84.3 | poor |
| German | AI-drawn | 5 | 0.4 | 1.0 | experimental at best (answer 6) |
| French | AI-drawn | 5 | 9.7 | 32.5 (`fr-04`) | experimental at best |
| Spanish | AI-drawn | 5 | 0.6 | 1.3 | experimental at best |
| Chinese | AI-drawn | 5 | 0.0 | 0.0 | experimental at best |
| Japanese | AI-drawn | 5 | 0.8 | 3.9 | experimental at best |

- Letter case is not counted on `ru-01` and `ru-02`, which are in block capitals (owner, 2026-11-28). With case counted, `ru-02` scores 71.1 and Russian 29.4.
- Russian block capitals and cursive differ: `ru-01` 1.6, the other four pages 17.4 to 33.9.
- The low figures on AI-drawn pages do not show that the models read real handwriting in those languages: several drawings look like fonts.
- A page has few characters and five pages are a small sample, so one page can move a language across a threshold.

## Proposal

1. Measure the shipped models first on the fixtures (task 1).
2. On-device spike in this order, stopping per language at the first that is good enough: PP-OCRv6 recognizers, TrOCR for English, TrOCR exports (Russian, then Spanish or French if poor).
3. How the user asks for handwriting: no automatic detection. If one model serves print and handwriting at least as well as the shipped ones, it replaces them and the UI does not change; otherwise a "Handwritten" switch.
4. Cloud engine: off by default, the user's own API key, nothing leaves the device without an explicit opt-in, no silent fallback.
5. Key storage, to be settled in task 8: encrypted with the Android Keystore; on the desktop a separate file in the data folder, readable by the user only, said so in the README; never in the log.
6. Correction (task 10): online reader only; off by default; one request returns both texts, so the text as written is always kept and the changes can be checked against it; translation, copy and save use the text shown. "As written" is the model's reading and may already contain fixes (see the measurements).
