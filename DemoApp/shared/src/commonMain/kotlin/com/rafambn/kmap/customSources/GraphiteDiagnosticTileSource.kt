package com.rafambn.kmap.customSources

import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.TileSource
import com.rafambn.kmap.mapSource.tiled.tiles.VectorTile
import com.rafambn.kmap.utils.vectorTile.MVTFeature
import com.rafambn.kmap.utils.vectorTile.MVTLayer
import com.rafambn.kmap.utils.vectorTile.MVTile
import com.rafambn.kmap.utils.vectorTile.RawMVTGeomType

class GraphiteDiagnosticTileSource : TileSource<VectorTile> {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<VectorTile> =
        TileResult.Success(VectorTile(zoom, row, column, diagnosticTile))
}

private val diagnosticTile = MVTile(
    layers = listOf(
        MVTLayer(
            name = "fills",
            extent = 4096,
            features = listOf(
                polygon(
                    300 to 300,
                    2500 to 300,
                    2500 to 2100,
                    300 to 2100,
                ),
                polygon(
                    1400 to 700,
                    3700 to 700,
                    3700 to 2200,
                    1400 to 2200,
                ),
            ),
        ),
        lineLayer("butt", listOf(400 to 2600, 1200 to 2600)),
        lineLayer("round-cap", listOf(1600 to 2600, 2400 to 2600)),
        lineLayer("square", listOf(2800 to 2600, 3600 to 2600)),
        lineLayer("miter", listOf(400 to 3500, 800 to 2900, 1200 to 3500)),
        lineLayer("round-join", listOf(1600 to 3500, 2000 to 2900, 2400 to 3500)),
        lineLayer("bevel", listOf(2800 to 3500, 3200 to 2900, 3600 to 3500)),
    ),
)

private fun polygon(vararg points: Pair<Int, Int>) = MVTFeature(
    id = null,
    type = RawMVTGeomType.POLYGON,
    geometry = listOf(points.toList()),
    properties = emptyMap(),
)

private fun lineLayer(name: String, points: List<Pair<Int, Int>>) = MVTLayer(
    name = name,
    extent = 4096,
    features = listOf(
        MVTFeature(
            id = null,
            type = RawMVTGeomType.LINESTRING,
            geometry = listOf(points),
            properties = emptyMap(),
        )
    ),
)
