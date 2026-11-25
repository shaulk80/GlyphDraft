package com.glyphdraft.engine.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class HebrewTimesTest {
    @Test
    fun theSpaceInsideATimeIsRemoved() {
        val cases: List<Pair<String, String>> =
            listOf(
                "החנות נפתחת ב-9: 00." to "החנות נפתחת ב-9:00.",
                "בשעה 14: 30" to "בשעה 14:30",
                "09: 05" to "09:05",
                "23: 59" to "23:59",
                "מ-8: 00 עד 17: 45" to "מ-8:00 עד 17:45",
            )

        for ((translation, expected) in cases) {
            assertEquals(expected, HebrewTimes.join("The source.", translation), "translation: $translation")
        }
    }

    @Test
    fun whatIsNotATimeStaysAsItIs() {
        val cases =
            listOf(
                "24: 00",
                "123: 45",
                "9: 75",
                "9: 5",
                "9: 000",
                "9:00",
                "סעיף: 12",
            )

        for (translation in cases) {
            assertEquals(translation, HebrewTimes.join("The source.", translation), "translation: $translation")
        }
    }

    @Test
    fun theSpacingOfTheSourceIsKept() {
        val joined = HebrewTimes.join("Chapter 3: 25 pages, from 9:00.", "פרק 3: 25 עמודים, מ-9: 00.")

        assertEquals("פרק 3: 25 עמודים, מ-9:00.", joined)
    }
}
