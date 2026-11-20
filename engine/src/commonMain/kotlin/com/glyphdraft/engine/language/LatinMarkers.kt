package com.glyphdraft.engine.language

import com.glyphdraft.core.Language

/**
 * What tells the Latin-script languages of the picker apart: very common words, letters that belong to one or two of
 * them, and apostrophe forms. Written for this project (function words of each language), so there is no model file
 * and no third-party license. A language is added by adding its [LatinMarkers] to [LatinLanguageScorer.default].
 *
 * Words that two languages share (`la` in French and Spanish, `a` in English and Spanish) are in both lists, so they
 * raise both scores and cancel out of the comparison.
 */
class LatinMarkers(
    val language: Language,
    val words: Set<String>,
    val letters: String,
    /** Text before an apostrophe that marks the language, with the apostrophe (French `l'`, `qu'`). */
    val elisions: Set<String> = emptySet(),
    /** Text after an apostrophe that marks the language (English `s`, `t`, `re`: it's, don't, they're). */
    val contractions: Set<String> = emptySet(),
) {
    companion object {
        val English =
            LatinMarkers(
                language = Language.English,
                words = wordsOf(
                    "the of and to in is that it was for on are as with his they be at this from or had by not but what all were we when your can there an which she you their if will would about out so has have been who its do does did my our me him her them these those than then into over only also just more most some any other such very how why where while because",
                ),
                letters = "",
                contractions = setOf("s", "t", "re", "ll", "ve", "d", "m"),
            )

        val German =
            LatinMarkers(
                language = Language.German,
                words = wordsOf(
                    "der die das und ist nicht ein eine zu den dem des mit auf für von sich im auch es an als wie bei oder aus nach wird sind ich sie er wir aber wenn noch nur über einen einem einer dass war hat werden kann um so durch vor zum zur dieser diese schon mehr sehr hier bitte ihr ihren ihre mich mir uns unser wurde wurden haben habe hatte sein seine ja nein doch",
                ),
                letters = "äöüß",
            )

        val French =
            LatinMarkers(
                language = Language.French,
                words = wordsOf(
                    "le la les des du de un une et est en que qui dans pour pas sur au aux avec ce cette ces il elle ils elles nous vous je tu ne se son sa ses par plus mais ou où comme été sont être avoir fait très tout leur leurs ont était aussi sans sous chez entre depuis votre vos notre nos mon ma mes ton ta tes si oui non bien",
                ),
                letters = "éèêëàâçîïôœùûÿ",
                elisions = setOf("l'", "d'", "j'", "n'", "s'", "c'", "m'", "t'", "qu'", "jusqu'", "lorsqu'", "puisqu'"),
            )

        val Spanish =
            LatinMarkers(
                language = Language.Spanish,
                words = wordsOf(
                    "el la los las de del que y en un una unos unas es por con para no se su sus al lo como más pero o este esta esto ese esa son está están muy ya también sin sobre entre cuando hay ser fue ha he ni yo tú él ella nosotros donde porque qué cómo dónde mi mis tu tus nuestro nuestra si sí bien todo todos todas",
                ),
                letters = "ñáéíóú¿¡",
            )

        private fun wordsOf(list: String): Set<String> = list.split(' ').filter { it.isNotEmpty() }.toSet()
    }
}
