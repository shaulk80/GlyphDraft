package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressShieldTest {
    private val seen = mutableListOf<String>()

    /** Records what it reads and writes it between angle brackets. */
    private val faithful: (String) -> Outcome<String> = { text ->
        seen.add(text)
        Outcome.Success("<$text>")
    }

    @Test
    fun theModelReadsAStandInForEveryAddressAndTheAddressComesBack() {
        val cases: List<Pair<String, String>> =
            listOf(
                "Mail a.b_c+d@x-y.co.uk, now." to "Mail Z19, now.",
                "Visit https://www.example.com/docs/page?id=42#top." to "Visit Z19.",
                "HTTP://EXAMPLE.ORG/ is down" to "Z19 is down",
                "See www.museum-berlin.de, room 2" to "See Z19, room 2",
                "(see www.a.de)" to "(see Z19)",
                "详情请发邮件至a@b.com。" to "详情请发邮件至Z19。",
                "כתבו ל-a@b.com היום" to "כתבו ל-Z19 היום",
                "Both a@b.com and c@d.net." to "Both Z19 and Z29.",
                "Room Z19: a@b.com" to "Room Z19: Z29",
            )

        for ((sentence, masked) in cases) {
            seen.clear()

            val translated = AddressShield.translate(sentence, faithful)

            assertEquals(listOf(masked), seen, "sentence: $sentence")
            assertEquals(Outcome.Success("<$sentence>"), translated, "sentence: $sentence")
        }
    }

    @Test
    fun textWithoutAnAddressGoesToTheModelAsItIs() {
        val cases = listOf("Hello.", "Meet @home, e.g. at 5.", "Awww.so nice", "user@localhost", "example.org")

        for (sentence in cases) {
            seen.clear()

            AddressShield.translate(sentence, faithful)

            assertEquals(listOf(sentence), seen, "sentence: $sentence")
        }
    }

    @Test
    fun aSentenceOfAddressesOnlyIsNotGivenToTheModel() {
        val cases = listOf("a@b.com", "www.a.de.", "a@b.com, https://c.de/x", "+1: a@b.com")

        for (sentence in cases) {
            assertEquals(Outcome.Success(sentence), AddressShield.translate(sentence, faithful), "sentence: $sentence")
        }
        assertEquals(emptyList(), seen)
    }

    @Test
    fun aModelThatLosesAStandInGetsTheTextAroundTheAddressesInPieces() {
        val spoilers: List<Pair<String, (String) -> String>> =
            listOf(
                "dropped" to { text -> text.replace("Z19", "") },
                "repeated" to { text -> text.replace("Z19", "Z19 Z19") },
                "changed" to { text -> text.replace("Z19", "z-19") },
            )

        for ((name, spoil) in spoilers) {
            val translated = AddressShield.translate("Contact: a@b.com, room b.") { Outcome.Success(spoil(it).uppercase()) }

            assertEquals(Outcome.Success("CONTACT: a@b.com, ROOM B."), translated, "stand-in $name")
        }
    }

    @Test
    fun aFailureOfTheModelIsPassedOn() {
        val failure = Outcome.Failure("out of memory")
        val onTheWholeSentence = AddressShield.translate("Mail a@b.com now.") { failure }
        var calls = 0
        val onAPiece =
            AddressShield.translate("Mail a@b.com now.") {
                calls++
                if (calls == 1) Outcome.Success("lost") else failure
            }

        assertEquals(failure, onTheWholeSentence)
        assertEquals(failure, onAPiece)
    }
}
