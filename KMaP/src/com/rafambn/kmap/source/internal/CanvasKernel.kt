package com.rafambn.kmap.source.internal

import com.rafambn.kmap.components.parameters.CanvasParameters
import com.rafambn.kmap.components.parameters.RasterCanvasParameters
import com.rafambn.kmap.components.parameters.VectorCanvasParameters
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.source.TileSpecs
import com.rafambn.kmap.geometry.plane.TilePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.math.ceil
import kotlin.math.floor

class CanvasKernel(
    val coroutineScope: CoroutineScope,
) {
    private val canvas = mutableMapOf<Int, CanvasEntry>()
    private var visibleTiles = emptyList<TileSpecs>()
    private var zoomLevel: Int? = null

    fun getActiveTiles(id: Int): ActiveTiles = canvas.getValue(id).engine.activeTiles

    internal fun resolveVisibleTiles(
        topLeft: TilePoint,
        bottomRight: TilePoint,
        zoomLevel: Int,
        mapProperties: MapProperties,
    ) {
        val visibleTiles = getVisibleTilesForLevel(
            topLeft, bottomRight, zoomLevel,
            mapProperties.tileRepeatMode, mapProperties.tileSize,
        )
        this.visibleTiles = visibleTiles
        this.zoomLevel = zoomLevel
        canvas.values.forEach { it.engine.renderTiles(visibleTiles, zoomLevel) }
    }

    /**
     * Cancels work owned by removed canvas IDs without cancelling [coroutineScope].
     * Cancellation is cooperative; this call does not wait for running tile sources to finish.
     * New canvases receive the last resolved tile selection, including an empty selection.
     */
    fun refreshCanvas(currentParameters: List<CanvasParameters>) {
        val currentIds = currentParameters.map { it.id }.toSet()

        val keysToRemove = canvas.keys.filter { it !in currentIds }
        keysToRemove.forEach {
            canvas.remove(it)?.job?.cancel()
        }

        currentParameters.forEach { parameter ->
            if (parameter.id !in canvas) {
                val job = Job(coroutineScope.coroutineContext[Job])
                val canvasScope = CoroutineScope(coroutineScope.coroutineContext + job)
                val renderer = when (parameter) {
                    is RasterCanvasParameters -> TileRenderer(
                        coroutineScope = canvasScope,
                        getTile = parameter.tileSource,
                        processTile = { it },
                    )
                    is VectorCanvasParameters -> TileRenderer(
                        coroutineScope = canvasScope,
                        getTile = parameter.tileSource,
                        processTile = { optimizeMVTile(it, parameter.style) },
                    )
                }
                val engine = CanvasEngine(parameter.maxCacheTiles, canvasScope, renderer)
                canvas[parameter.id] = CanvasEntry(engine, job)
                zoomLevel?.let { engine.renderTiles(visibleTiles, it) }
            }
        }
    }
}

internal fun getVisibleTilesForLevel(
    topLeft: TilePoint,
    bottomRight: TilePoint,
    zoomLevel: Int,
    tileRepeatMode: TileRepeatMode,
    tileDimension: TileDimension,
): List<TileSpecs> {
    require(zoomLevel in 0..30) { "Supported zoom levels are 0..30" }
    if (topLeft.x >= bottomRight.x || topLeft.y >= bottomRight.y) return emptyList()

    val tileCount = 1L shl zoomLevel
    var minX = floor(topLeft.x / tileDimension.width.value * tileCount).toLong()
    // Right and bottom edges are exclusive: a tile that only touches them is not visible.
    var maxX = (ceil(bottomRight.x / tileDimension.width.value * tileCount) - 1).toLong()
    var minY = floor(topLeft.y / tileDimension.height.value * tileCount).toLong()
    var maxY = (ceil(bottomRight.y / tileDimension.height.value * tileCount) - 1).toLong()
    if (tileRepeatMode == TileRepeatMode.NONE) {
        minX = maxOf(minX, 0L)
        maxX = minOf(maxX, tileCount - 1)
        minY = maxOf(minY, 0L)
        maxY = minOf(maxY, tileCount - 1)
    }
    if (minX > maxX || minY > maxY) return emptyList()
    require(minX >= Int.MIN_VALUE && maxX <= Int.MAX_VALUE &&
        minY >= Int.MIN_VALUE && maxY <= Int.MAX_VALUE
    ) { "Visible tile indices exceed the supported Int range" }
    val visibleTiles = mutableListOf<TileSpecs>()
    for (x in minX..maxX)
        for (y in minY..maxY)
            visibleTiles.add(TileSpecs(zoomLevel, y.toInt(), x.toInt()))
    return visibleTiles
}
