package com.rafambn.mvtparser

data class ParsedMVTile(
    val layers: List<ParsedLayer>
)

data class ParsedLayer(
    val name: String,
    val extent: Int,
    val features: List<ParsedFeature>
)

data class ParsedFeature(
    val id: Long?,
    val type: GeomType,
    val geometry: List<List<Pair<Int, Int>>>,
    val properties: Map<String, Any?>
)
