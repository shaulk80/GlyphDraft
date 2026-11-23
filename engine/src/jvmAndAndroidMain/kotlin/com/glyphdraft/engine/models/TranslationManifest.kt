package com.glyphdraft.engine.models

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.TranslationDirection

/**
 * The files of one translation direction (an OPUS-MT model): [config] and [vocabulary] give the special token ids, the
 * [tokenizer] the pieces and scores, [encoder] and [decoder] are the int8 ONNX graphs. A [decoder] without a key/value
 * cache ([decoderHasCache] false) is read again from the start of the sentence at each step.
 */
data class TranslationModelFiles(
    val direction: TranslationDirection,
    val config: ModelFile,
    val vocabulary: ModelFile,
    val tokenizer: ModelFile,
    val encoder: ModelFile,
    val decoder: ModelFile,
    val decoderHasCache: Boolean,
) {
    val all: List<ModelFile> get() = listOf(config, vocabulary, tokenizer, encoder, decoder)
}

/**
 * The translation models the app can download (decisions 0012 and 0013): one [ModelPack] per direction, pinned like the
 * recognition models of [ModelManifest] (URL at a fixed commit, SHA-256, size), all of them OPUS-MT by Helsinki-NLP
 * (CC-BY 4.0, credits in the README and in Settings). Most come from the `Xenova` conversions; the two Hebrew directions
 * come from one private uploader without a license text, so they are marked experimental. A removed repository breaks
 * first-time installs of its direction; the pins refuse any changed file. To change a model, change revision, hashes and
 * sizes together; `RealDownloadTest` downloads one of them from the real server.
 *
 * On disk: `<models>/mt/<from>-<to>/<file name>`, the file names as in the repository (`decoder_model_merged_quantized.onnx`
 * has the cache, `decoder_model_quantized.onnx` has not). English to Japanese and English to Hebrew are not offered (no
 * working model, decision 0012); Hebrew to Russian and the other Hebrew exports follow in task 6.
 */
object TranslationManifest {
    private const val XENOVA = "https://huggingface.co/Xenova"
    private const val UPLOADER = "https://huggingface.co/R4kSo1997"

    private const val MERGED_DECODER = "decoder_model_merged_quantized.onnx"
    private const val PLAIN_DECODER = "decoder_model_quantized.onnx"
    private const val ENCODER = "encoder_model_quantized.onnx"

    private val experimentalDirections =
        setOf(
            TranslationDirection(Language.English, Language.Chinese),
            TranslationDirection(Language.Hebrew, Language.English),
            TranslationDirection(Language.Russian, Language.Hebrew),
        )

    private val models: List<TranslationModelFiles> =
        listOf(
            xenova(
                Language.English, Language.German, "opus-mt-en-de", "1ca130c44c4c5441ef16d48aae521a424ab644f7",
                config = Digest("517955ebf66cd6523f9982851dd41771b74fb2d9c00b4933ba2922ac7a63336f", 1_411),
                vocabulary = Digest("d5acea957b265a78554999144459c5e391e0df525864edc8287bc090290baa44", 1_389_436),
                tokenizer = Digest("8e0fcf45621ea87fa680c7f9969c37a7f819c1f4c7658a2e6e0879b866a14b17", 5_498_450),
                encoder = Digest("15834b45fabd2dfb8c6c029b3ca3e7289aeefd90ece798ce42bcf548d1bd3b8d", 49_366_942),
                decoder = Digest("8b46a825964cdd182fe47cc780f0fb87d357eae1f47cf20aa63c1c09be5b510c", 56_652_404),
            ),
            xenova(
                Language.German, Language.English, "opus-mt-de-en", "399dfd68706739fffd503f876093e455ae268a06",
                config = Digest("6754743ec4754957d8552dad869d1314f3af6626b67446ec4cebe5e2fbaa0fc4", 1_376),
                vocabulary = Digest("d5acea957b265a78554999144459c5e391e0df525864edc8287bc090290baa44", 1_389_436),
                tokenizer = Digest("8e0fcf45621ea87fa680c7f9969c37a7f819c1f4c7658a2e6e0879b866a14b17", 5_498_450),
                encoder = Digest("4cedda8f8c89b72a42b3c6cd1e7a27f2de24457093e3bf80cb3e46829641fcd8", 49_366_942),
                decoder = Digest("4712800584b2d77c2f9e620bf29523e2b20b62f65ade5011ee01780e2b0ea5d3", 56_652_404),
            ),
            xenova(
                Language.English, Language.French, "opus-mt-en-fr", "28726206f80896b90035bd99cccd5cc1e151f916",
                config = Digest("b522b73fcc86f77981349c1df2a2e041eed0dbcbea4acf2635019cf21d51ffc0", 1_411),
                vocabulary = Digest("f2ba9c69ae20f96b8bd821239a9152be422394f980350b77907cffc183db5f2d", 1_458_196),
                tokenizer = Digest("8391785c1a2139e7af4678571ccd8dc654ecbb72e4be186940f65d7c604f0246", 5_637_839),
                encoder = Digest("0a81bdba62f53223740a8c6c6f58e716eba8ea1f92dfa569caf27ed5e3b6a0f7", 50_090_398),
                decoder = Digest("333b244bce16023df04541c8cf9fd60aec9b0569da393c4b831d561897b0bda8", 57_381_512),
            ),
            xenova(
                Language.French, Language.English, "opus-mt-fr-en", "6b166a182780e118c997879d0ad5be4b53671644",
                config = Digest("6c2851f154d7b88c7767e4fdcf8a5694a36a456bfa6cbcde1b13fdbade3b56f6", 1_411),
                vocabulary = Digest("f2ba9c69ae20f96b8bd821239a9152be422394f980350b77907cffc183db5f2d", 1_458_196),
                tokenizer = Digest("8391785c1a2139e7af4678571ccd8dc654ecbb72e4be186940f65d7c604f0246", 5_637_839),
                encoder = Digest("e727cb26ac6bf816394c49671af69c9ae5798868fed1e40849872415dffc1772", 50_090_398),
                decoder = Digest("73bc7ac8e29c42e6f212ebcc29a2991d3646a04aabb642c0036aa54b40e4e1a9", 57_381_512),
            ),
            xenova(
                Language.English, Language.Spanish, "opus-mt-en-es", "4b002a4c7edd54a7ced58877258b87f7efd3f892",
                config = Digest("499eae1f97eeba63172fc884f92fd727ae803b7ceb79c0f653aa4b011152af2f", 1_468),
                vocabulary = Digest("b074b4cca0036ade5a39ea97faabd534e1015482c480fc2cb02c6481983eb163", 1_720_044),
                tokenizer = Digest("285eb29e7155ee48851a77960797813f86a125f70d2c1a124f613f1fbd2b19c3", 6_262_682),
                encoder = Digest("03434edd1147e6c49e22066e9dc5888235e99cc35a6711d1c4ea5b06455d5da9", 52_899_742),
                decoder = Digest("58ee5ddd6e22d1693d8722b90c1486afb93700a4376cf28fd74175052ad530ae", 60_212_804),
            ),
            xenova(
                Language.Spanish, Language.English, "opus-mt-es-en", "eadfd7c658a9d8929ac3b8e996b68a68e2c7d480",
                config = Digest("fab3a7f93185bc5aa7b419f6a1e6e74d98c8f2a506c94493d3019bf46da3478d", 1_433),
                vocabulary = Digest("b074b4cca0036ade5a39ea97faabd534e1015482c480fc2cb02c6481983eb163", 1_720_044),
                tokenizer = Digest("285eb29e7155ee48851a77960797813f86a125f70d2c1a124f613f1fbd2b19c3", 6_262_682),
                encoder = Digest("c01e70f8455efc350831aa2af7ed187b16829241f0018ba07d1ba643a391bc18", 52_899_742),
                decoder = Digest("4cd91ab30240d295ce907b5100826031838c672005e57e524d711122d75605fa", 60_212_804),
            ),
            xenova(
                Language.English, Language.Russian, "opus-mt-en-ru", "e050376e960175e8ddc5cff85025dc8436cddd68",
                config = Digest("db16bf0ea7bac239273214c3b9ab44577884227371a9a25e0191b7baf8d6f167", 1_376),
                vocabulary = Digest("5cf0d95d930d8d3e783c9e2f46a72f08b43a18060dab4ddefbcb66a733efedcb", 2_726_796),
                tokenizer = Digest("981cd3d9fef6bb4dda8082afda716b65deb6717cb3ef35ac0b57002c09dec2bb", 7_205_388),
                encoder = Digest("97f28d8295d231b7da721ded06fa8c034787df2122cd8f85b904abdc4d15bd53", 51_628_446),
                decoder = Digest("7efefcd793a5663bc204224a92a123524f3dfe2ffad59e5f73d6734d1b11ad04", 58_931_576),
            ),
            xenova(
                Language.Russian, Language.English, "opus-mt-ru-en", "afe8c6c738ec81b6d033fd8f44f9678a639a7c67",
                config = Digest("60dc7d0906dbe42a1d740cdd7e84ce1534685b81fbe16005a0301adac69086ce", 1_376),
                vocabulary = Digest("5cf0d95d930d8d3e783c9e2f46a72f08b43a18060dab4ddefbcb66a733efedcb", 2_726_796),
                tokenizer = Digest("981cd3d9fef6bb4dda8082afda716b65deb6717cb3ef35ac0b57002c09dec2bb", 7_205_388),
                encoder = Digest("c10df0466540534bb70c4402d539c349b8bfa9968184cad89f51408cf13e7eb7", 51_628_446),
                decoder = Digest("25e7ff97b201bce64ab9c39749761d657b70b8602bcbc1ce01c67f713e90ffdf", 58_931_576),
            ),
            xenova(
                Language.English, Language.Chinese, "opus-mt-en-zh", "046f55aec303cdee3e0318604406d4df20f1e8ea",
                config = Digest("4727d1229a04f95bf6f39abf949d8080615433d99d6ebd85f81c09edd247d5fa", 1_503),
                vocabulary = Digest("22c957348eed495ee925afc40a36da3e387c8a34a734c8486967c2dca271613e", 1_747_795),
                tokenizer = Digest("d0c7da27056e8f42adce9e76d8e792e5daa64e15f5acd2e7aabf0121877dd4c1", 6_380_952),
                encoder = Digest("d3b7912bf6a9bd27e4c074c2df91d4ff3d5b4bc5f7f6c8d7cc9c805c98fbafee", 52_899_742),
                decoder = Digest("023be4f841f4c47cd65fffcbaa81c0d99d7f7e0138f7ba0e03fa220a4e688aff", 60_212_804),
            ),
            xenova(
                Language.Chinese, Language.English, "opus-mt-zh-en", "39d480d52a9ea3065a1f117adfe4dbc55de10e6f",
                config = Digest("293d318fce41dbf04114eac45037bb88a32d7c4ee21011a75e24a8b98ca45ad1", 1_389),
                vocabulary = Digest("08a119a1defd522fa047cb5e3bfe3e89633e96caa38ced0dc9cee7ef1021a011", 1_747_906),
                tokenizer = Digest("b306d0301cf280bfd647d7067b5ade2a97b987e6d678df110703c002433643ff", 6_381_339),
                encoder = Digest("84d5e171b626bc8b6b220d022ac58696e9528c25deeacca62b5cbf4364547a99", 52_899_742),
                decoder = Digest("c6b7f04ff1ba0fbd1bf6852599b4c0cad6fe512d57cd887f44ef36cf705424cb", 60_212_804),
            ),
            xenova(
                Language.Japanese, Language.English, "opus-mt-ja-en", "1a906cfaaf7c8f4193f67f5885c082aa6dbd9d16",
                config = Digest("d35476f1d3858883feec67e5a63ddc387452a70fda9c8e26db99c71aa2ad6a34", 1_376),
                vocabulary = Digest("a1667b43d021e09c76f385101bd473f20b5691a12cdca7dda43e4a8daff20021", 1_622_775),
                tokenizer = Digest("770ff2855437cf44f1f110550c5a9dca773253a167aeac36076b2073d259aa3b", 5_991_485),
                encoder = Digest("345262b16bcdda1468b0f3380c112b7ce79f731176b4b1d21f6edd5b2ae0d25c", 50_705_822),
                decoder = Digest("b304d0014e4e1575437b6af95467b6cb54405d923732d8359113bd6dbbee93c0", 58_001_744),
            ),
            uploader(
                Language.Hebrew, Language.English, "opus-mt-he-en-onnx-int8", "c6bc7b589abef1f34b3212a854a3c643e03413ee",
                config = Digest("892164c33ac7490b5e17e695f56195721f9b209b310e4ff0d4d9c41f05aab923", 1_058),
                vocabulary = Digest("db635b5b0c106224a0768da8cbc36b45c20073008926ad9f2eca7363df3c26c7", 2_133_162),
                tokenizer = Digest("b903bfd46e7b4d2a3cad75054c78150a055e80421c343f429141e45d9d7d3b81", 1_657_614),
                encoder = Digest("4dddddaf9883129cad257f5c05ff5a544af95b78faa610f2689235280fbc239d", 139_016_126),
                decoder = Digest("91256f6c5107a0c78be732d2c25fed6877d465754882182ec32a68b944a611f8", 226_942_497),
            ),
            uploader(
                Language.Russian, Language.Hebrew, "opus-mt-ru-he-onnx-int8", "a0dd9f98273cec644f5ec23f68aba52a0924801e",
                config = Digest("26a8851796a66a9cb2b5b0a173902c61e4df36cbeabcf66d4befd440607e459c", 1_420),
                vocabulary = Digest("07ebbd85f1c119df8554a947d22972dec486522f5c39d3186e96fcdc3634e111", 3_292_463),
                tokenizer = Digest("183feec3e2d4280c91039821b358572f416cdcc18b41e270732c50c7bc809e09", 1_931_793),
                encoder = Digest("8e7f5cb2c15e463004ffd1b433898df6b9eafea94168e9731e46ee780c9d095c", 52_024_019),
                decoder = Digest("dd494f39b94cbe74f8636201c60aee97315e940557b30bb10414ea48df1379ac", 91_683_022),
            ),
        )

    /** One pack per direction, in the order the UI shows them. Add these to [ModelManifest.packs] in the composition root. */
    val packs: List<PackManifest> = models.map(::packOf)

    /** The files of [direction]; null when the app has no model for it. */
    fun filesOf(direction: TranslationDirection): TranslationModelFiles? = models.firstOrNull { it.direction == direction }

    /** The pack id of [direction], folder-safe and stable: `mt-en-de`. */
    fun packId(direction: TranslationDirection): String = "mt-${direction.from.code}-${direction.to.code}"

    private class Digest(val sha256: String, val sizeBytes: Long)

    private fun packOf(model: TranslationModelFiles): PackManifest {
        val direction = model.direction
        val pack =
            ModelPack(
                id = packId(direction),
                displayName = "${direction.from.displayName} to ${direction.to.displayName}",
                languages = emptySet(),
                sizeBytes = model.all.sumOf { it.sizeBytes },
                direction = direction,
                experimental = direction in experimentalDirections,
            )
        return PackManifest(pack, model.all)
    }

    private fun xenova(
        from: Language,
        to: Language,
        repo: String,
        revision: String,
        config: Digest,
        vocabulary: Digest,
        tokenizer: Digest,
        encoder: Digest,
        decoder: Digest,
    ): TranslationModelFiles {
        val base = "$XENOVA/$repo/resolve/$revision"
        return build(from, to, config, vocabulary, tokenizer, encoder, decoder, MERGED_DECODER, true) { remote ->
            if (remote.endsWith(".onnx")) "$base/onnx/$remote" else "$base/$remote"
        }
    }

    private fun uploader(
        from: Language,
        to: Language,
        repo: String,
        revision: String,
        config: Digest,
        vocabulary: Digest,
        tokenizer: Digest,
        encoder: Digest,
        decoder: Digest,
    ): TranslationModelFiles {
        val base = "$UPLOADER/$repo/resolve/$revision"
        return build(from, to, config, vocabulary, tokenizer, encoder, decoder, PLAIN_DECODER, false) { remote -> "$base/$remote" }
    }

    private fun build(
        from: Language,
        to: Language,
        config: Digest,
        vocabulary: Digest,
        tokenizer: Digest,
        encoder: Digest,
        decoder: Digest,
        decoderName: String,
        decoderHasCache: Boolean,
        urlOf: (String) -> String,
    ): TranslationModelFiles {
        val direction = TranslationDirection(from, to)
        val folder = "mt/${from.code}-${to.code}"

        fun file(name: String, digest: Digest) = ModelFile("$folder/$name", urlOf(name), digest.sha256, digest.sizeBytes)

        return TranslationModelFiles(
            direction = direction,
            config = file("config.json", config),
            vocabulary = file("vocab.json", vocabulary),
            tokenizer = file("tokenizer.json", tokenizer),
            encoder = file(ENCODER, encoder),
            decoder = file(decoderName, decoder),
            decoderHasCache = decoderHasCache,
        )
    }
}
