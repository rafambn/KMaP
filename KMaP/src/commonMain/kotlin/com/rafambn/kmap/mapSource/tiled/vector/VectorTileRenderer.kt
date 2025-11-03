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
            else if (mvTile is VectorTileResult.Success) {
                tilesProcessResult.send(OptimizedVectorTileResult.Success(optimizeMVTile(mvTile, style)))
            }

//            val fullTilePath = Path().apply {
//                moveTo(0f, 0f)
//                lineTo(4096f, 0f)
//                lineTo(4096f, 4096f)
//                lineTo(0f, 4096f)
//                close()
//            }
//
//            val randomColor = generateTileColor(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)
//
//            val feature = OptimizedRenderFeature(
//                geometry = OptimizedGeometry.Polygon(listOf(fullTilePath)),
//                properties = emptyMap(),
//                paintProperties = OptimizedPaintProperties(
//                    fillColor = randomColor,
//                    fillOpacity = 1f
//                )
//            )
//            val optimizedData = OptimizedMVTile(
//                extent = 4096,
//                renderFeature = listOf(feature)
//            )
//            val optimizedTile = OptimizedVectorTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, optimizedData)
//
//            tilesProcessResult.send(OptimizedVectorTileResult.Success(optimizedTile))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
        }
    }

    private fun optimizeMVTile(mvtile: VectorTileResult.Success, style: Style): OptimizedVectorTile {
        val tile = mvtile.tile
        val mvtData = tile.mvtile ?: return OptimizedVectorTile(tile.zoom, tile.row, tile.col, null)

        val renderLayers = mutableListOf<OptimizedRenderFeature>()

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
            renderLayers.addAll(optimizedFeatures)
        }

        val optimizedData = OptimizedMVTile(
            extent = mvtData.layers.firstOrNull()?.extent ?: 4096,
            renderFeature = renderLayers
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
                    ring.map { (x, y) -> Pair(x.toFloat() / extent, y.toFloat() / extent) }
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
                OptimizedPaintProperties(
                    fillColor = extractColorProperty(styleLayer.paint, "fill-color", Color(0xFF0000)),
                    fillOpacity = extractOpacityProperty(styleLayer.paint, "fill-opacity", 1.0).toFloat(),
                    fillOutlineColor = extractColorProperty(styleLayer.paint, "fill-outline-color", Color.Black)
                )
            }

            "line" -> {
                OptimizedPaintProperties(
                    lineColor = extractColorProperty(styleLayer.paint, "line-color", Color.Black),
                    lineWidth = extractNumberProperty(styleLayer.paint, "line-width", 1.0).toFloat(),
                    lineOpacity = extractOpacityProperty(styleLayer.paint, "line-opacity", 1.0).toFloat(),
                    lineCap = extractStringProperty(styleLayer.layout, "line-cap", "butt"),
                    lineJoin = extractStringProperty(styleLayer.layout, "line-join", "miter")
                )
            }

            "symbol" -> {
                OptimizedPaintProperties(
                    textField = extractStringProperty(styleLayer.layout, "text-field", ""),
                    textSize = extractNumberProperty(styleLayer.layout, "text-size", 12.0).toFloat(),
                    textColor = extractColorProperty(styleLayer.paint, "text-color", Color.Black),
                    textOpacity = extractOpacityProperty(styleLayer.paint, "text-opacity", 1.0).toFloat()
                )
            }//TODO has a lot of other logic's here

            else -> OptimizedPaintProperties()
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
