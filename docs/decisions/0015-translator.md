# 0015: The on-device translator

Date: 2026-11-23. Status: proposed (M3 task 5; the owner accepts by merging the pull request). Follows 0012 (engine), 0013 (types) and 0014 (manifest and routes).

- **Problem.** The `Translator` of `core` needs an implementation that cuts the text, runs the models of a route and puts the text back, without keeping several 100 MB models in memory.

## Decision

1. **Layers.** `OnDeviceTranslator` (shared code, `engine/translation`) is the `Translator`: checks the route and the installed packs, segments the text with `TextSegmenter`, runs the sentences through the directions of the route one after the other, joins them with `SegmentedText.join`. A `DirectionTranslator` (tokenizer, `MarianConfig`, `SentenceModel`) translates the sentences of one direction. A `DirectionSource` opens a direction; `OnnxDirectionSource` (jvm and android) builds it from the files of the `TranslationManifest`. The orchestration is tested on every target with fakes; only `OnnxSentenceModel` and its two decoders need ONNX Runtime.
2. **One direction open at a time.** A model is about 500 MB while loaded (spike). Opening the next direction closes the previous one first; the last one stays open for the next call (the same direction again costs no load) until `close()`. Calls run one after the other (a mutex), on `Dispatchers.Default`.
3. **Special ids come from the model, never from a constant.** `MarianConfigReader` reads start, end and pad ids from `config.json`; the tokenizer takes its ids from the vocabulary; `OnnxDirectionSource` refuses files where they disagree (end, pad, or start id), so mixed-up files are a failure at load, not nonsense output. The spike hard-coded end id 0; for `he-en` (end id 25568) that ended the source with a wrong token and never stopped on the real end id. Measured again with the right ids, `he to en` is chrF 78.9, not 82.5.
4. **Decode loop: greedy, one sentence at a time.** Two decoders behind `GreedyDecoder`: the merged decoder with key/value cache (the `Xenova` models; the result of a step is closed as soon as the next step has read it, so the cache does not pile up) and the plain decoder of the Hebrew exports (whole prefix every step). The id of the padding token is never chosen. The limit is twice the source length plus ten, within the model's `max_length` and positions. **Not done: beam search and batching of sentences.** The spike measured 55 to 63 tokens per second with one sentence at a time; batching needs padded masks and a batched cache and gains little on a CPU with 4 threads. Both can be added behind `SentenceModel` without touching the callers.
5. **Cancellation.** The decode loop and the sentence loop check the coroutine context between steps; a cancelled job stops at the next token and the models are closed or reused as usual. This is the one place where a `CancellationException` is thrown on purpose (coroutine rule, not an error).
6. **Failures are values.** Blank text, a pair without a route, a missing model (named in the message), a model that does not load or fails while running are `Outcome.Failure`. A sentence longer than the model's positions (the segmenter keeps them far below) is cut at the end, keeping the end id.
7. **Not wired yet.** `OnDeviceEngine` and the composition roots get the translator in task 7, with the Android run (memory, time).

## Found on the way

- `OnDeviceOcrEngine` took every installed pack of the catalog for a reading pack. With translation packs in the same catalog (0013) a pair of models would have been "probed" as a recognizer and the read would have failed; it now looks at packs without a direction only (test `anInstalledTranslationModelIsNotAReadingPack`). The apps would have hit this in task 7.

## Test

`FixtureTranslationTest` (opt-in, like the OCR fixtures) translates the 30 reference sentences through the whole translator and prints chrF against the spike figure; more than 2 points under it is a WARNING, more than 5 under fails. CI downloads `mt-en-de`, `mt-de-en` and `mt-en-fr` (about 350 MB, cached); every other installed direction is measured too, and all 13 directions and four pivot pairs matched the spike to the first decimal (except `he to en`, point 3).
