package com.rafambn.kmap.utils.vectorTile

data class MVTile(
    val layers: List<MVTLayer>
)

data class MVTLayer(
    val name: String,
    val extent: Int,
    val features: List<MVTFeature>
)

data class MVTFeature(
    val id: Long?,
    val type: RawMVTGeomType,
    val geometry: List<List<Pair<Int, Int>>>,
    val properties: Map<String, Any?>
)
