package com.glyphdraft.engine.models

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack

/**
 * One file of a model pack. [path] is relative to the models folder and uses '/'; files that several packs need (the
 * text detector) have the same [path] in each pack and are downloaded once. [sha256] is lower-case hex.
 */
data class ModelFile(val path: String, val url: String, val sha256: String, val sizeBytes: Long)

/** A [pack] and the [files] that have to be on disk for it to work. */
data class PackManifest(
    val pack: ModelPack,
    val files: List<ModelFile>,
)

/**
 * The models the app can download, pinned: every URL names a fixed revision of the model repository and every file
 * has the SHA-256 it had when it was reviewed, so a changed file on the server is refused, not installed. To update a
 * model, change revision, hash and size here together (docs/spikes/m2-ocr-engines.md lists how they were measured).
 * All of these are Apache 2.0.
 */
object ModelManifest {
    private const val PADDLE_BASE = "https://huggingface.co/PaddlePaddle"
    private const val TESSERACT_BASE = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast"

    private const val DET_REPO = "PP-OCRv5_mobile_det_onnx"
    private const val DET_REVISION = "e6f4fa85f00e168c862bc462aebca69eef9b3d3d"
    private const val CJK_REPO = "PP-OCRv5_mobile_rec_onnx"
    private const val CJK_REVISION = "ed152b8b495f84de93cda5709d768548a9127622"
    private const val LATIN_REPO = "latin_PP-OCRv5_mobile_rec_onnx"
    private const val LATIN_REVISION = "89d3a50e2c27e2e7cceeab0e944c25c807d5db4f"
    private const val ESLAV_REPO = "eslav_PP-OCRv5_mobile_rec_onnx"
    private const val ESLAV_REVISION = "9a32171fc5718746875e1a261818884517975013"
    private const val TESSDATA_FAST_REVISION = "923915d4ced2a7235221788285785a29c4a42d4a"

    /** Pack ids are folder-safe and stable; the apps and the engines refer to them. */
    const val LATIN = "latin"
    const val ESLAV = "eslav"
    const val CJK = "cjk"
    const val HEBREW = "hebrew"

    /** Text detection shared by every PP-OCRv5 pack. */
    val detectorModel = paddleFile("ppocrv5/det/inference.onnx", DET_REPO, DET_REVISION, "inference.onnx", "a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d", 4_826_518)
    val detectorConfig = paddleFile("ppocrv5/det/inference.yml", DET_REPO, DET_REVISION, "inference.yml", "98069072e1b6b37d727fd9d9f11725faa46d6ea0de012f2ed26caea011c37699", 903)

    val latinModel = paddleFile("ppocrv5/latin/inference.onnx", LATIN_REPO, LATIN_REVISION, "inference.onnx", "7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498", 8_042_023)
    val latinConfig = paddleFile("ppocrv5/latin/inference.yml", LATIN_REPO, LATIN_REVISION, "inference.yml", "0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84", 6_817)

    val eslavModel = paddleFile("ppocrv5/eslav/inference.onnx", ESLAV_REPO, ESLAV_REVISION, "inference.onnx", "b3018ef2b09a0250b6e0c8e871c927098363e5fd4df890cc68e8358eb0aaf1bd", 7_887_627)
    val eslavConfig = paddleFile("ppocrv5/eslav/inference.yml", ESLAV_REPO, ESLAV_REVISION, "inference.yml", "025039bac23eb4a308efcefa4d58eab3af440767815c6ba6938468bf6353ee5a", 4_538)

    val cjkModel = paddleFile("ppocrv5/cjk/inference.onnx", CJK_REPO, CJK_REVISION, "inference.onnx", "da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092", 16_534_782)
    val cjkConfig = paddleFile("ppocrv5/cjk/inference.yml", CJK_REPO, CJK_REVISION, "inference.yml", "5dfeb2777f6d0db8177d8128a8acfcf6e6276dc4ac73ea3bf0dc06d6a5e85d8e", 148_345)

    val hebrewData =
        ModelFile(
            path = "tessdata/heb.traineddata",
            url = "$TESSERACT_BASE/$TESSDATA_FAST_REVISION/heb.traineddata",
            sha256 = "11f9e43ab227f786352a50f75c94c2e9906f1baba86d93276da19da7ce0904db",
            sizeBytes = 961_404,
        )

    /** In the order the UI shows them. The download size of a pack includes the shared detector. */
    val packs: List<PackManifest> =
        listOf(
            packOf(LATIN, "Latin scripts", setOf(Language.English, Language.German, Language.French, Language.Spanish), listOf(detectorModel, detectorConfig, latinModel, latinConfig)),
            packOf(ESLAV, "Russian", setOf(Language.Russian), listOf(detectorModel, detectorConfig, eslavModel, eslavConfig)),
            packOf(CJK, "Chinese and Japanese", setOf(Language.Chinese, Language.Japanese), listOf(detectorModel, detectorConfig, cjkModel, cjkConfig)),
            packOf(HEBREW, "Hebrew", setOf(Language.Hebrew), listOf(hebrewData)),
        )

    private fun packOf(id: String, displayName: String, languages: Set<Language>, files: List<ModelFile>): PackManifest =
        PackManifest(ModelPack(id, displayName, languages, files.sumOf { it.sizeBytes }), files)

    private fun paddleFile(path: String, repo: String, revision: String, remoteName: String, sha256: String, sizeBytes: Long): ModelFile =
        ModelFile(path, "$PADDLE_BASE/$repo/resolve/$revision/$remoteName", sha256, sizeBytes)
}
