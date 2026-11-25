# 0021: Which line breaks of a page are meant

Date: 2026-11-25. Status: proposed (M3 task 11; the owner accepts by merging the pull request). Follows 0008 (OCR pipeline), 0010 (Hebrew) and 0015 (translator).

- **Problem.** The readers gave one line per row of the image and never an empty line, and the segmenter joined everything up to an empty line. A page was one paragraph: line breaks were lost, and headings and list lines glued into sentences vanished from the translation (hand-test findings 1 and 2). Translating line by line does not work either: a sentence cut at a wrap lost words.

## Decision

1. **Two texts** in `RecognitionResult`. `text` keeps the lines of the page and has an empty line at a gap between blocks; it is shown, copied and saved as the original. `runningText` has the wrapped lines joined; it is what gets translated. The translator takes every line break of its input as meant.
2. **The places of the lines decide** (`PageLayout`, shared code, over `PlacedLine`). Chosen over guessing from the number of characters in a line, which cannot see gaps and is unreliable with proportional fonts and Chinese or Japanese text (owner, 2026-11-25).
3. **A gap** between two lines is a step down of more than 2.8 line heights, or of more than 1.5 times the usual step of the page and more than 1.7 line heights, or a step up (next column). The line height is the upper quartile over the page.
4. **Other breaks**, in this order: a list mark (bullet, dash or number followed by a space) or a colon at the end of the upper line keeps the break; a sentence end keeps it, unless the line is inside a wrapped paragraph and had no room for the next word; a next line in lower case is a wrap; a line at least 12 line heights long with no room left before the right margin of the page for the next word (two characters in Chinese or Japanese) is a wrap; everything else keeps the break.
5. **PP-OCRv5 lines** are levelled by the median tilt of the long boxes first. Rows are formed again after reading, because an empty speck could split a row ("3 kg" came out on a line of its own).
6. **Hebrew** lines come from Tesseract's line boxes, with the edges mirrored. Its paragraphs are not used (0010: it splits where the page has no gap).

## Measured and not covered

The numbers were set on pages of the hand test and on pages drawn for this task (English, German, Russian, Chinese, Japanese, Hebrew; line spacing 1.05 to 2.0, and a photo), all of which come out as laid out. That is the data they were fitted to; other layouts are untried. Known limits: the longest line of a page of long unpunctuated lines can be joined with the next one; a wide heading directly above a list can take its first item; a paragraph much narrower than the widest line of the page is only joined where it goes on in lower case; pages in columns are read row by row as before (0008), and table columns are not restored.
