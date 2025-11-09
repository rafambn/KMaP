package com.rafambn.kmap.mapSource.tiled.engine

import androidx.compose.ui.geometry.Offset
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.CoroutineScope
import kotlin.math.pow

class CanvasKernel(
    val coroutineScope: CoroutineScope
) {

    val canvas = mutableMapOf<Int, CanvasEngine<*>>()

    fun getActiveTiles(id: Int): ActiveTiles = canvas.getValue(id).activeTiles.value

    fun resolveVisibleTiles(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        val visibleTiles = getVisibleTilesForLevel(
            viewPort,
            zoomLevel,
            mapProperties.outsideTiles,
            mapProperties.tileSize
        )
        canvas.forEach { (_, canvasEngine) -> canvasEngine.renderTiles(visibleTiles, zoomLevel) }
    }

    fun refreshCanvas(currentParameters: List<CanvasParameters>) {
        val currentIds = currentParameters.map { it.id }.toSet()

        val keysToRemove = canvas.keys.filter { it !in currentIds }
        keysToRemove.forEach { canvas.remove(it) }

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

    private fun getVisibleTilesForLevel(
        viewPort: ViewPort,
        zoomLevel: Int,
        outsideTilesType: OutsideTilesType,
        tileDimension: TileDimension
    ): List<TileSpecs> {
        val topLeftTile = getXYTile(
            viewPort.topLeft,
            zoomLevel,
            tileDimension
        )
        val topRightTile = getXYTile(
            viewPort.topRight,
            zoomLevel,
            tileDimension
        )
        val bottomRightTile = getXYTile(
            viewPort.bottomRight,
            zoomLevel,
            tileDimension
        )
        val bottomLeftTile = getXYTile(
            viewPort.bottomLeft,
            zoomLevel,
            tileDimension
        )
        val horizontalTileIntRange =
            IntRange(
                minOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first),
                maxOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first)
            )
        val verticalTileIntRange =
            IntRange(
                minOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second),
                maxOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second)
            )

        val visibleTileSpecs = mutableListOf<TileSpecs>()
        if (outsideTilesType == OutsideTilesType.NONE) {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange) {
                    var xTile: Int
                    if (x < 0 || x > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        xTile = x
                    var yTile: Int
                    if (y < 0 || y > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        yTile = y
                    visibleTileSpecs.add(TileSpecs(zoomLevel, yTile, xTile))
                }
        } else {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange)
                    visibleTileSpecs.add(TileSpecs(zoomLevel, y, x))
        }
        return visibleTileSpecs
    }

    private fun getXYTile(position: Offset, zoomLevel: Int, tileDimension: TileDimension): Pair<Int, Int> = Pair(
        (position.x / tileDimension.width * (1 shl zoomLevel)).toIntFloor(),
        (position.y / tileDimension.height * (1 shl zoomLevel)).toIntFloor()
    )
}
