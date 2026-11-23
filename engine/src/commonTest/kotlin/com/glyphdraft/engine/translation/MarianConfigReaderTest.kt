package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarianConfigReaderTest {
    private val xenovaStyle =
        """
        {"d_model": 512, "decoder_attention_heads": 8, "decoder_layers": 6, "decoder_start_token_id": 58100,
         "eos_token_id": 0, "pad_token_id": 58100, "max_length": 512, "max_position_embeddings": 512, "num_beams": 4}
        """.trimIndent()

    private val uploaderStyle =
        """
        {"d_model": 1024, "decoder_attention_heads": 16, "decoder_layers": 6, "decoder_start_token_id": 60268,
         "eos_token_id": 25568, "pad_token_id": 60268, "max_length": null, "max_position_embeddings": 1024, "forced_eos_token_id": null}
        """.trimIndent()

    @Test
    fun readsTheNumbersOfAnOptimumExport() {
        val expected = MarianConfig(58100, 0, 58100, layers = 6, heads = 8, headSize = 64, maxPositions = 512, maxLength = 512)

        assertEquals(Outcome.Success(expected), MarianConfigReader.read(xenovaStyle))
    }

    @Test
    fun theSpecialIdsAreTheOnesOfTheModelNotConstants() {
        val config = (MarianConfigReader.read(uploaderStyle) as Outcome.Success).value

        assertEquals(60268, config.startId)
        assertEquals(60268, config.padId)
        assertEquals(25568, config.endId)
        assertEquals(64, config.headSize)
    }

    @Test
    fun aNullMaxLengthFallsBackToThePositions() {
        val config = (MarianConfigReader.read(uploaderStyle) as Outcome.Success).value

        assertEquals(1024, config.maxLength)
    }

    @Test
    fun anUnreadableConfigIsAFailureWithAMessage() {
        val cases =
            listOf(
                "not json",
                "[1, 2]",
                "{}",
                xenovaStyle.replace("\"eos_token_id\": 0,", ""),
                xenovaStyle.replace("\"d_model\": 512", "\"d_model\": 500"),
                xenovaStyle.replace("\"decoder_attention_heads\": 8", "\"decoder_attention_heads\": 0"),
            )

        for (json in cases) {
            assertIs<Outcome.Failure>(MarianConfigReader.read(json), json)
        }
    }

    @Test
    fun theGenerationLimitIsTwiceTheSourcePlusTenWithinTheModelLimits() {
        val config = MarianConfig(1, 0, 1, layers = 6, heads = 8, headSize = 64, maxPositions = 100, maxLength = 60)
        val cases = listOf(1 to 12, 5 to 20, 25 to 60, 400 to 60)

        for ((sourceLength, expected) in cases) {
            assertEquals(expected, config.generationLimit(sourceLength), "source length $sourceLength")
        }
    }
}
