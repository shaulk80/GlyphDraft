# Handwriting fixtures

Photographed or drawn pages of handwriting (`<name>.jpg`), the text written on each (`<name>.txt`) and `manifest.tsv`. `HandwritingBaselineTest` reads them with the shipped models and reports the character error rate; accuracy never fails a test.

## Manifest columns

| Column | Values |
|---|---|
| `name` | `<language>-<number>`, for example `he-01`; a page beside the numbered set has a word instead, like `he-school` |
| `language` | `en`, `ru`, `he`, `de`, `fr`, `es`, `zh`, `ja` |
| `source` | `real` (written by a person) or `ai` (drawn by an image model: synthetic, reported separately) |
| `writing` | `print` (print-like letters) or `cursive` (joined or running hand) |
| `paper` | `lined` or `plain` |
| `case` | `exact` (a reading must keep the letter case of the text) or `either` (capitals and regular case are both right, for a page written in capitals) |

The goal is at least 5 pages per language, with print-like and cursive writing on lined and plain paper.

## Adding a page

1. Make the image with `java scripts/fixtures/PrepareHandwriting.java <input> engine/src/test/resources/fixtures/handwriting/<name>.jpg`. It turns the page upright, limits the long side to 2048 pixels and writes a JPEG without metadata; never copy a photo in directly. Look at the result.
2. Write the expected text as it is on the page, in logical order, one line per written line, UTF-8. Keep the writer's mistakes; write a blank line to be filled in as underscores.
3. Proofread the text against the image. For a drawn page an AI agent may do this: it gets the image and the intended text, reports every difference, and the expected text follows the image; list the changes below.
4. Add the manifest row, and for a real page note the consent below. No names, addresses or other personal data on a page.

## Pages

| Pages | Origin and consent | Proofread |
|---|---|---|
| `he-school` | A school assignment, photographed; the owner consents to publishing it as a test fixture (2026-11-27). | By the owner, 2026-11-27 |
| `en-01` to `en-05`, `ru-01` to `ru-05`, `he-01` to `he-05` | Handwritten by the owner from texts written for this project, photographed and cropped to the text; the owner consents to publishing them as test fixtures (2026-11-28). | Drafted by an AI agent from the images, verified by the owner (the writer), 2026-11-28 |
| `de`, `fr`, `es`, `zh`, `ja`, `01` to `05` each | Synthetic: drawn with Gemini in "Auto" mode (exact submodel unknown) from texts written for this project, 2026-11-28. | By an AI agent against the intended texts, 2026-11-28 |

## Where a text differs from what was asked

The texts the pages were made from, and what was asked for each page, are in `docs/manual/m4-samples.md`. Here "asked" is that text; the expected text follows the image. Quote and dash shapes and spaces are not listed.

Drawn pages:

| Page | Difference |
|---|---|
| `de-04` | Line breaks: "14. März" starts the second line, "drei" ends the fourth. |
| `de-05` | Line breaks: "Milch" starts the third line, "Alles gut verrühren, dann in" is the fourth. |
| `es-02` | Line breaks: seven lines where four were asked. |
| `es-04` | Line breaks: six lines where five were asked. |
| `fr-05` | "ét" for "et" (an accent is drawn over the e). Strokes through the "a" of "farine" and through "œ" are not letters and are left out. |

The other drawn pages show the asked text.

Handwritten pages:

| Page | Difference |
|---|---|
| `en-01` | "have one" for "own one"; "seeds of" for "seeds or". |
| `ru-01` | "семена и рассаду" for "семена или рассаду". |
| `ru-02` | A colon after the heading. |
| `ru-05` | "две больших" for "две большие". |
| `he-02` | A colon after the heading. |
| `he-03` | No full stop ends the three rules. |
| `he-05` | The writer's mistake, kept: חבילות (havilot) for חביות (haviot). |

`ru-01` and `ru-02` are written in block capitals, hence `either` in the `case` column. In `he-01` the last letter of the second word looks like a final kaf; the writer wrote a vav, and the text has the vav.

## Writing column

`writing` follows the page, not the request: `fr-01` is cursive, `zh-03`, `zh-05`, `ja-03` and `ja-04` are print-like. No drawn page 05 is really untidy.
