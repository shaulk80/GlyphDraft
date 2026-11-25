package com.glyphdraft.engine.translation

/**
 * The model that writes Hebrew puts a space into a time ("9: 00", issue #45), which reads as a wrong time. The space is
 * removed, unless the model's source wrote the same text itself ("Chapter 3: 25 pages").
 */
internal object HebrewTimes {
    private val SPACED_TIME = Regex("""(?<!\d)([01]?\d|2[0-3]): ([0-5]\d)(?!\d)""")

    /** [translation] is what the model made of [source]. */
    fun join(source: String, translation: String): String =
        SPACED_TIME.replace(translation) { time ->
            if (time.value in source) {
                time.value
            } else {
                "${time.groupValues[1]}:${time.groupValues[2]}"
            }
        }
}
