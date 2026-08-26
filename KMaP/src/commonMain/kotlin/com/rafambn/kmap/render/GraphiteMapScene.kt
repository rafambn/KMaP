package com.rafambn.kmap.render

internal data class GraphiteMapScene(
    val camera: GraphiteMapCamera,
    val canvases: List<GraphiteVectorCanvas>,
)
