package com.glyphdraft.desktop

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TextSaver

/** Temporary until the file dialogs are wired (M1 task 5): always "picks" a tiny fake image. */
class PlaceholderImagePicker : ImagePicker {
    override suspend fun pick(): Outcome<ImageInput?> = Outcome.Success(ImageInput("sample.png", byteArrayOf(0)))
}

/** Temporary until the file dialogs are wired (M1 task 5). */
class PlaceholderTextSaver : TextSaver {
    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> = Outcome.Failure("Saving is not wired yet.")
}
