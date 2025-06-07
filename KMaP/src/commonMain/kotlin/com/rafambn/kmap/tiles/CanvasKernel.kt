package com.rafambn.kmap.tiles

import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import kotlinx.coroutines.CoroutineScope

class CanvasKernel(
    val coroutineScope: CoroutineScope
) {

    val canvas = mutableMapOf<Int, CanvasEngine>()

    fun getTileLayers(id: Int): TileLayers = canvas.getValue(id).tileLayers

    fun renderTile(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        canvas.forEach { (_, canvasEngine) -> canvasEngine.renderTile(viewPort, zoomLevel, mapProperties) }
    }

    fun refreshCanvas(currentParameters: List<CanvasParameters>) {
        // Get the set of current parameter IDs
        val currentIds = currentParameters.map { it.id }.toSet()

        // Remove canvas engines that are not in currentParameters
        val keysToRemove = canvas.keys.filter { it !in currentIds }
        keysToRemove.forEach { canvas.remove(it) }

        // Add new canvas engines for parameters not already in canvas
        currentParameters.forEach { parameters ->
            if (parameters.id !in canvas) {
                canvas[parameters.id] = CanvasEngine(
                    parameters.maxCacheTiles,
                    parameters.getTile,
                    coroutineScope
                )
            }
        }
    }
}
