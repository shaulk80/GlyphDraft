package com.glyphdraft.ui.state

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack

/**
 * What the user should know about translating one pair: the languages the text passes through between source and
 * target ([via], empty for a direct pair), the models still to download ([missing]) and whether any model of the route
 * is experimental (weak quality or unchecked, decision 0012).
 */
data class RouteInfo(
    val via: List<Language>,
    val missing: List<ModelPack>,
    val experimental: Boolean,
) {
    /** "via English" or "via English and Russian"; null for a direct pair. */
    val viaPhrase: String?
        get() = if (via.isEmpty()) null else "via " + via.joinToString(" and ") { it.displayName }

    /** Short remarks for a picker entry, in the order the user needs them. */
    val notes: List<String>
        get() =
            listOfNotNull(
                viaPhrase,
                "download needed".takeIf { missing.isNotEmpty() },
                "experimental".takeIf { experimental },
            )
}

/** The route from [from] to [to] over the models the app knows, installed or not; null when the pair has none. */
fun AppState.routeInfo(from: Language, to: Language): RouteInfo? {
    val steps = translationRoutes.steps(from, to)
    if (steps == null) {
        return null
    }

    val routePacks = steps.mapNotNull { step -> packs.firstOrNull { it.direction == step } }
    return RouteInfo(
        via = steps.dropLast(1).map { it.to },
        missing = routePacks.filterNot { isInstalled(it) },
        experimental = routePacks.any { it.experimental },
    )
}

/**
 * The name of [target] in the pickers. With a known [source] it tells how the pair is translated: "Hebrew (via
 * Russian, download needed, experimental)". Without one (auto-detect) the route is not known yet and only the name shows.
 */
fun AppState.targetLabel(target: Language, source: Language?): String {
    if (source == null) {
        return target.displayName
    }

    val info = routeInfo(source, target)
    if (info == null) {
        return "${target.displayName} (not available)"
    }

    if (info.notes.isEmpty()) {
        return target.displayName
    }

    return "${target.displayName} (${info.notes.joinToString(", ")})"
}
