package com.glyphdraft.core

/**
 * Which language pairs can be translated, given the directions that have a model (decision 0012). A pair without a model
 * of its own goes through other languages: the shortest chain of directions is used, which makes English the hub of every
 * pair and Russian the second hub of Hebrew (de to he = de, en, ru, he). Whether the models are installed is not asked
 * here; see [ModelCatalog.packsForRoute].
 *
 * [directions] are taken in order; between chains of the same length the one found first (by that order) wins, so the
 * result does not depend on hashing.
 */
class TranslationRoutes(directions: Collection<TranslationDirection>) {
    private val directionList = directions.distinct()
    private val next: Map<Language, List<TranslationDirection>> = directionList.groupBy { it.from }

    /** Languages the text can be in. */
    val sources: Set<Language> = directionList.mapTo(linkedSetOf()) { it.from }

    /** Languages the text can be turned into. */
    val targets: Set<Language> = directionList.mapTo(linkedSetOf()) { it.to }

    /**
     * The directions to run one after the other to get from [from] to [to]; empty when they are the same language, null
     * when there is no chain.
     */
    fun steps(from: Language, to: Language): List<TranslationDirection>? {
        if (from == to) {
            return emptyList()
        }

        val cameBy = mutableMapOf<Language, TranslationDirection>()
        val queue = ArrayDeque<Language>()
        queue.add(from)
        while (queue.isNotEmpty() && to !in cameBy) {
            val language = queue.removeFirst()
            for (direction in next[language].orEmpty()) {
                if (direction.to != from && direction.to !in cameBy) {
                    cameBy[direction.to] = direction
                    queue.add(direction.to)
                }
            }
        }

        if (to !in cameBy) {
            return null
        }

        return generateSequence(cameBy[to]) { cameBy[it.from] }.toList().asReversed()
    }

    /** The languages the text passes through, [from] first and [to] last (one entry when they are equal); null when there is no chain. */
    fun route(from: Language, to: Language): List<Language>? {
        val steps = steps(from, to)
        if (steps == null) {
            return null
        }

        return listOf(from) + steps.map { it.to }
    }

    /** Languages that can be reached from [from] (not [from] itself). */
    fun targetsFrom(from: Language): Set<Language> = targets.filterTo(linkedSetOf()) { it != from && steps(from, it) != null }
}

/** The routes over every translation model the catalog knows, installed or not. */
fun ModelCatalog.translationRoutes(): TranslationRoutes = TranslationRoutes(translationPacks().mapNotNull { it.direction })

/**
 * The models a translation from [from] to [to] runs, in order of use, installed or not (filter with [isInstalled] to
 * find what is still to download). Empty when no model is needed, null when the pair has no route.
 */
fun ModelCatalog.packsForRoute(from: Language, to: Language): List<ModelPack>? {
    val steps = translationRoutes().steps(from, to)
    if (steps == null) {
        return null
    }

    val packs = steps.map { packFor(it) }
    if (packs.any { it == null }) {
        return null
    }

    return packs.filterNotNull()
}
