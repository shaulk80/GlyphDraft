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

The goal is at least 5 pages per language, with print-like and cursive writing on lined and plain paper.

## Adding a page

1. Make the image with `java scripts/fixtures/PrepareHandwriting.java <input> engine/src/test/resources/fixtures/handwriting/<name>.jpg`. It turns the page upright, limits the long side to 2048 pixels and writes a JPEG without metadata; never copy a photo in directly. Look at the result.
2. Write the expected text as it is on the page, in logical order, one line per written line, UTF-8. Keep the writer's mistakes; write a blank line to be filled in as underscores.
3. Proofread the text against the image. For a drawn page an AI agent may do this: it gets the image and the intended text, reports every difference, and the expected text follows the image; list the changes below.
4. Add the manifest row, and for a real page note the consent below. No names, addresses or other personal data on a page.

## Pages

| Page | Origin and consent | Proofread |
|---|---|---|
| `he-school` | A school assignment, photographed; the owner consents to publishing it as a test fixture (2026-11-27). | By the owner, 2026-11-27 |
