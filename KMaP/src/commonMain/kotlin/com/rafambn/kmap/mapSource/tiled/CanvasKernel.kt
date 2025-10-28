package com.rafambn.kmap.mapSource.tiled

import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapSource.tiled.raster.RasterCanvasEngine
import com.rafambn.kmap.mapSource.tiled.vector.VectorCanvasEngine
import kotlinx.coroutines.CoroutineScope
import kotlin.collections.forEach

class CanvasKernel(
    val coroutineScope: CoroutineScope
) {

    val canvas = mutableMapOf<Int, CanvasEngine>()

    fun getTileLayers(id: Int): TileLayers = canvas.getValue(id).tileLayers.value

    fun resolveVisibleTiles(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        val visibleTiles = TileFinder.getVisibleTilesForLevel(
            viewPort,
            zoomLevel,
            mapProperties.outsideTiles,
            mapProperties.tileSize
        )
        canvas.forEach { (_, canvasEngine) -> canvasEngine.renderTiles(visibleTiles, zoomLevel) }
    }

    fun refreshCanvas(currentParameters: List<CanvasParameters>) {
        // Get the set of current parameter IDs
        val currentIds = currentParameters.map { it.id }.toSet()

        // Remove canvas engines that are not in currentParameters
        val keysToRemove = canvas.keys.filter { it !in currentIds }
        keysToRemove.forEach { canvas.remove(it) }

        // Add new canvas engines for parameters not already in canvas
        currentParameters.forEach { parameter ->
            if (parameter.id !in canvas) {
                if (parameter is RasterCanvasParameters) {
                    canvas[parameter.id] = RasterCanvasEngine(
                        parameter.maxCacheTiles,
                        parameter.tileSource,
                        coroutineScope
                    )
                } else if (parameter is VectorCanvasParameters) {
                    canvas[parameter.id] = VectorCanvasEngine(
                        parameter.maxCacheTiles,
                        parameter.tileSource,
                        coroutineScope,
                        parameter.style
                    )
                }
            }
        }
    }
}
