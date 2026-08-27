package com.rafambn.kmap.render

/** Selects the renderer used for one complete KMaP instance. */
enum class MapRenderBackend {
    Auto,
    Compose,
    Graphite,
}

internal fun resolveGraphiteBackend(
    renderBackend: MapRenderBackend,
    contentIncompatibility: String?,
    graphiteFailed: Boolean,
): Boolean = when (renderBackend) {
    MapRenderBackend.Auto ->
        contentIncompatibility == null && !graphiteFailed

    MapRenderBackend.Compose -> false
    MapRenderBackend.Graphite -> {
        check(contentIncompatibility == null) { contentIncompatibility.orEmpty() }
        true
    }
}
