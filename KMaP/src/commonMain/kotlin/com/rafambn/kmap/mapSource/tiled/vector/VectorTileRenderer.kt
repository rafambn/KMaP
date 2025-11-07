package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.graphics.Color
import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.style.Style
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.vectorTile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import kotlin.collections.emptyList

class VectorTileRenderer(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    val style: Style
) {
    val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    val tilesProcessedChannel = Channel<OptimizedVectorTile>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<OptimizedVectorTileResult>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.launch(Dispatchers.Default + SupervisorJob()) {
            val specsBeingProcessed = mutableListOf<TileSpecs>()
            val tilesBeingProcessed = mutableListOf<TileSpecs>()

            while (isActive) {
                select {
                    tilesToProcessChannel.onReceive { tilesToProcess ->
                        tilesToProcess.forEach { specs ->
                            val loopedSpecs = TileSpecs(
                                specs.zoom,
                                specs.row.loopInZoom(specs.zoom),
                                specs.col.loopInZoom(specs.zoom)
                            )
                            if (!specsBeingProcessed.contains(specs)) {
                                specsBeingProcessed.add(specs)
                                if (!tilesBeingProcessed.contains(loopedSpecs)) {
                                    tilesBeingProcessed.add(loopedSpecs)
                                    worker(loopedSpecs, workerResultChannel)
                                }
                            }
                        }
                    }
                    workerResultChannel.onReceive { tileResult ->
                        when (tileResult) {
                            is OptimizedVectorTileResult.Success -> {
                                tilesProcessedChannel.send(tileResult.tile)
                                tilesBeingProcessed.remove(tileResult.tile as TileSpecs)
                                specsBeingProcessed.removeAll {
                                    TileSpecs(
                                        it.zoom,
                                        it.row.loopInZoom(it.zoom),
                                        it.col.loopInZoom(it.zoom)
                                    ) == tileResult.tile
                                }
                            }

                            is OptimizedVectorTileResult.Failure -> {
                                tilesBeingProcessed.remove(tileResult.specs)
                                specsBeingProcessed.removeAll {
                                    TileSpecs(
                                        it.zoom,
                                        it.row.loopInZoom(it.zoom),
                                        it.col.loopInZoom(it.zoom)
                                    ) == tileResult.specs
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessResult: SendChannel<OptimizedVectorTileResult>
    ) = launch(Dispatchers.Default) {
        try {
            val mvTile = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)
            if (mvTile is VectorTileResult.Failure)
                tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
            else if (mvTile is VectorTileResult.Success)
                tilesProcessResult.send(OptimizedVectorTileResult.Success(optimizeMVTile(mvTile, style)))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
        }
    }

    private fun optimizeMVTile(mvtile: VectorTileResult.Success, style: Style): OptimizedVectorTile {
        val tile = mvtile.tile
        val mvtData = tile.mvtile ?: return OptimizedVectorTile(tile.zoom, tile.row, tile.col, null)

        val layerFeatures = mutableMapOf<String, MutableList<OptimizedRenderFeature>>()
        val extent = mvtData.layers.firstOrNull()?.extent ?: 4096

        style.layers.forEach { styleLayer ->
            if (styleLayer.type == "background") return@forEach

            if (!isLayerVisibleAtZoom(styleLayer, tile.zoom)) return@forEach

            val sourceLayerName = styleLayer.sourceLayer ?: return@forEach
            val mvtLayer = mvtData.layers.find { it.name == sourceLayerName } ?: return@forEach

            val optimizedFeatures = mvtLayer.features.mapNotNull { feature ->
                if (!matchesFilter(feature, styleLayer.filter)) return@mapNotNull null

                val isValidGeometry = when (styleLayer.type) {
                    "fill" -> feature.type == RawMVTGeomType.POLYGON
                    "line" -> feature.type == RawMVTGeomType.LINESTRING
                    "symbol" -> feature.type == RawMVTGeomType.POINT
                    else -> false
                }

                if (!isValidGeometry) return@mapNotNull null

                val geometry = buildOptimizedGeometry(feature, mvtLayer.extent) ?: return@mapNotNull null

                val paintProperties = resolvePaintProperties(styleLayer, feature)

                OptimizedRenderFeature(
                    geometry = geometry,
                    properties = feature.properties,
                    paintProperties = paintProperties
                )
            }

            layerFeatures[styleLayer.id] = optimizedFeatures.toMutableList()
        }

        val optimizedData = OptimizedMVTile(
            extent = extent,
            layerFeatures = layerFeatures,
            backgroundFeature = null
        )

        return OptimizedVectorTile(tile.zoom, tile.row, tile.col, optimizedData)
    }

    private fun buildOptimizedGeometry(
        feature: MVTFeature,
        extent: Int
    ): OptimizedGeometry? {
        return when (feature.type) {
            RawMVTGeomType.POLYGON -> {
                val paths = feature.geometry.map { ring -> buildPolygonPathFromGeometry(listOf(ring), extent) }
                OptimizedGeometry.Polygon(paths)
            }

            RawMVTGeomType.LINESTRING -> {
                val path = buildPathFromGeometry(feature.geometry, extent)
                OptimizedGeometry.LineString(path)
            }

            RawMVTGeomType.POINT -> {
                val coordinates = feature.geometry.flatMap { ring ->
                    ring.map { (x, y) -> Pair(x.toFloat(), y.toFloat()) }
                }
                OptimizedGeometry.Point(coordinates)
            }

            else -> null
        }
    }

    private fun resolvePaintProperties(
        styleLayer: StyleLayer,
        feature: MVTFeature
    ): OptimizedPaintProperties {
        return when (styleLayer.type) {
            "fill" -> {
                OptimizedPaintProperties.Fill(
                    color = extractColorProperty(styleLayer.paint, "fill-color", Color.Magenta),
                    opacity = extractOpacityProperty(styleLayer.paint, "fill-opacity", 1.0).toFloat(),
                    outlineColor = extractColorProperty(styleLayer.paint, "fill-outline-color", Color.Transparent)
                )
            }

            "line" -> {
                OptimizedPaintProperties.Line(
                    color = extractColorProperty(styleLayer.paint, "line-color", Color.Magenta),
                    width = extractNumberProperty(styleLayer.paint, "line-width", 1.0).toFloat(),
                    opacity = extractOpacityProperty(styleLayer.paint, "line-opacity", 1.0).toFloat(),
                    cap = extractStringProperty(styleLayer.layout, "line-cap", "butt"),
                    join = extractStringProperty(styleLayer.layout, "line-join", "miter")
                )
            }

            "background" -> {
                OptimizedPaintProperties.Background(
                    color = extractColorProperty(styleLayer.paint, "background-color", Color.Magenta),
                    opacity = extractOpacityProperty(styleLayer.paint, "background-opacity", 1.0).toFloat()
                )
            }

            "symbol" -> {
                OptimizedPaintProperties.Symbol(
                    field = extractStringProperty(styleLayer.layout, "text-field", ""),
                    size = extractNumberProperty(styleLayer.layout, "text-size", 12.0).toFloat(),
                    color = extractColorProperty(styleLayer.paint, "text-color", Color.Magenta),
                    opacity = extractOpacityProperty(styleLayer.paint, "text-opacity", 1.0).toFloat()
                )
            }//TODO has a lot of other logic's here

            else -> OptimizedPaintProperties.Fill() // Default to empty fill properties
        }
    }

    private fun generateTileColor(zoom: Int, row: Int, col: Int): Color {
        val seed = (zoom.toLong() * 31 + row.toLong()) * 31 + col.toLong()
        val random = kotlin.random.Random(seed)

        val r = random.nextFloat()
        val g = random.nextFloat()
        val b = random.nextFloat()

        return Color(r, g, b)
    }
}
