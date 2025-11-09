package com.rafambn.kmap.mapSource.tiled.vector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.rafambn.kmap.mapSource.tiled.OptimizedVectorTile
import com.rafambn.kmap.mapSource.tiled.TileSpecs
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.vectorTile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select

class VectorTileRenderer(
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    coroutineScope: CoroutineScope,
    val optimizedStyle: OptimizedStyle
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
                tilesProcessResult.send(OptimizedVectorTileResult.Success(optimizeMVTile(mvTile, optimizedStyle)))
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(OptimizedVectorTileResult.Failure(tileToProcess))
        }
    }

    private fun optimizeMVTile(mvtile: VectorTileResult.Success, optimizedStyle: OptimizedStyle): OptimizedVectorTile {
        val tile = mvtile.tile
        val mvtData = tile.mvtile ?: return OptimizedVectorTile(tile.zoom, tile.row, tile.col, null)

        val layerFeatures = mutableMapOf<String, MutableList<OptimizedRenderFeature>>()
        val extent = mvtData.layers.firstOrNull()?.extent ?: 4096

        optimizedStyle.layers.forEach { optimizedStyleLayer ->
            if (optimizedStyleLayer.type == "background") return@forEach

            val sourceLayerName = optimizedStyleLayer.sourceLayer ?: return@forEach
            val mvtLayer = mvtData.layers.find { it.name == sourceLayerName } ?: return@forEach

            val optimizedFeatures = mvtLayer.features.mapNotNull { feature ->
                val featureProperties = feature.properties.filterValues { it != null } as Map<String, Any>
                val geometryType = when (feature.type) {
                    RawMVTGeomType.POINT -> "Point"
                    RawMVTGeomType.LINESTRING -> "LineString"
                    RawMVTGeomType.POLYGON -> "Polygon"
                    else -> "Unknown"
                }
                val featureId = feature.id

                if (optimizedStyleLayer.filter?.evaluate(featureProperties, geometryType, featureId) == false) return@mapNotNull null

                val isValidGeometry = when (optimizedStyleLayer.type) {
                    "fill" -> feature.type == RawMVTGeomType.POLYGON
                    "line" -> feature.type == RawMVTGeomType.LINESTRING
                    "symbol" -> feature.type == RawMVTGeomType.POINT
                    else -> false
                }

                if (!isValidGeometry) return@mapNotNull null

                val geometry = buildOptimizedGeometry(feature) ?: return@mapNotNull null

                OptimizedRenderFeature(
                    geometry = geometry,
                    properties = featureProperties
                )
            }

            layerFeatures[optimizedStyleLayer.id] = optimizedFeatures.toMutableList()
        }

        val optimizedData = OptimizedMVTile(
            extent = extent,
            layerFeatures = layerFeatures
        )

        return OptimizedVectorTile(tile.zoom, tile.row, tile.col, optimizedData)
    }

    private fun buildOptimizedGeometry(feature: MVTFeature): OptimizedGeometry? {
        return when (feature.type) {
            RawMVTGeomType.POLYGON -> {
                val paths = feature.geometry.map { ring -> buildPathFromGeometry(listOf(ring), true) }
                OptimizedGeometry.Polygon(paths)
            }

            RawMVTGeomType.LINESTRING -> {
                val path = buildPathFromGeometry(feature.geometry, false)
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
}


private fun buildPathFromGeometry(geometry: List<List<Pair<Int, Int>>>, isClosed: Boolean): Path {
    val path = Path()

    geometry.forEach { ring ->
        if (ring.isEmpty()) return@forEach

        val (startX, startY) = ring.first()
        path.moveTo(startX.toFloat(), startY.toFloat())

        ring.drop(1).forEach { (x, y) ->
            path.lineTo(x.toFloat(), y.toFloat())
        }

        if (isClosed)
            path.close()
    }

    return path
}

