package com.glyphdraft.engine.language

import com.glyphdraft.core.Language
import com.glyphdraft.core.Script
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextLanguageDetectorTest {
    private val detector = TextLanguageDetector()
    private val all = Language.Supported.toSet()

    @Test
    fun readsTheLanguageOfASentence() =
        runTest {
            val cases =
                listOf(
                    "The meeting starts at ten, and everyone is welcome to join us." to Language.English,
                    "It's the end of the road, and they don't know what to do." to Language.English,
                    "Die Besprechung beginnt am Donnerstag, bitte bringen Sie Ihren Ausweis mit." to Language.German,
                    "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich." to Language.German,
                    "La réunion commence à dix heures, et tout le monde est invité à venir." to Language.French,
                    "J’ai oublié l’adresse de l’hôtel où nous avons dormi." to Language.French,
                    "La reunión empieza a las diez y todos están invitados a venir." to Language.Spanish,
                    "¿Dónde está el baño? Mañana vamos a la playa con los niños." to Language.Spanish,
                    "Собрание начинается в десять часов, приходите все." to Language.Russian,
                    "הפגישה מתחילה בעשר בבוקר וכולם מוזמנים להצטרף" to Language.Hebrew,
                    "会议将于上午十点开始，欢迎大家参加。" to Language.Chinese,
                    "会議は午前十時に始まります。皆さんご参加ください。" to Language.Japanese,
                )

            for ((text, expected) in cases) {
                val guess = detector.detect(text, all)
                assertNotNull(guess, "no guess for: $text")
                assertEquals(expected, guess.language, "text: $text")
                assertTrue(guess.confidence > 0.0 && guess.confidence <= 1.0, "confidence ${guess.confidence} for: $text")
            }
        }

    @Test
    fun theMajorityScriptDecidesInMixedText() =
        runTest {
            val russianMajority = "Hello, мир! This sign reads Добро пожаловать.\nExit / Выход, gate 7 / ворота 7."
            val englishMajority = "Exit to the main street and turn left at the bank, then ask for Иван."
            assertEquals(Language.Russian, detector.detect(russianMajority, all)?.language)
            assertEquals(Language.English, detector.detect(englishMajority, all)?.language)
        }

    @Test
    fun answersOnlyWithACandidate() =
        runTest {
            val cases: List<Triple<String, Set<Language>, Language?>> =
                listOf(
                    Triple("Собрание начинается в десять часов.", setOf(Language.English, Language.German), null),
                    Triple("Die Besprechung beginnt am Donnerstag, bitte bringen Sie Ihren Ausweis mit.", setOf(Language.English, Language.French), null),
                    Triple("Die Besprechung beginnt am Donnerstag, bitte bringen Sie Ihren Ausweis mit.", setOf(Language.English, Language.German), Language.German),
                    Triple("会议将于上午十点开始。", setOf(Language.Japanese), Language.Japanese),
                    Triple("会议将于上午十点开始。", setOf(Language.Chinese, Language.Japanese), Language.Chinese),
                    Triple("会議は始まります", setOf(Language.Chinese), null),
                    Triple("The meeting starts at ten.", emptySet(), null),
                )

            for ((text, candidates, expected) in cases) {
                assertEquals(expected, detector.detect(text, candidates)?.language, "text: $text, candidates: $candidates")
            }
        }

    @Test
    fun cannotTellWhenThereIsNothingToGoOn() =
        runTest {
            val cases =
                listOf(
                    "",
                    "   \n",
                    "1234 5678 !?",
                    "Wasser",
                    "ΑΒΓΔ αβγδ",
                    "la",
                )

            for (text in cases) {
                assertNull(detector.detect(text, all), "text: '$text'")
            }
        }

    @Test
    fun aTieBetweenLanguagesIsNotAGuess() =
        runTest {
            // "la" is a word of both French and Spanish and nothing else in the text points to either.
            assertNull(detector.detect("la la", setOf(Language.French, Language.Spanish)))
        }

    @Test
    fun moreEvidenceMeansMoreConfidence() =
        runTest {
            val short = detector.detect("Das ist gut", all)
            val long = detector.detect("Das ist ein sehr guter Text, und er wird von dem Leser auch gern gelesen.", all)
            assertNotNull(short)
            assertNotNull(long)
            assertTrue(long.confidence > short.confidence, "${long.confidence} should be above ${short.confidence}")
        }

    @Test
    fun aLanguageWithoutMarkersIsNeverChosenAmongSeveral() =
        runTest {
            val unknownLatin = Language("it", "Italian", Script.LATIN)
            val guess = detector.detect("The meeting starts at ten, and everyone is welcome.", setOf(Language.English, unknownLatin))
            assertEquals(Language.English, guess?.language)
        }
}
