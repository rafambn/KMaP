package com.rafambn.kmap.tiles

import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import kotlinx.coroutines.CoroutineScope

class CanvasKernel(
    val coroutineScope: CoroutineScope
) {

    val canvas = mutableMapOf<Int, CanvasEngine>()

    fun addCanvas(parameters: CanvasParameters) {
        canvas.getOrPut(parameters.id) { CanvasEngine(parameters.maxCacheTiles, parameters.getTile, coroutineScope) }
        //TODO invalidade old canvas
    }

    fun getTileLayers(id: Int): TileLayers = canvas.getValue(id).tileLayers

    fun renderTile(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        canvas.forEach { (_, canvasEngine) -> canvasEngine.renderTile(viewPort, zoomLevel, mapProperties) }
    }
}
