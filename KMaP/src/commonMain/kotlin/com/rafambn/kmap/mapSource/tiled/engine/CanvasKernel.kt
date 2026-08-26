package com.rafambn.kmap.mapSource.tiled.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.pow

class CanvasKernel(
    val coroutineScope: CoroutineScope,
    val mapState: MapState
) {

    private val canvas = mutableMapOf<Int, CanvasEngine<*>>()
    private val canvasParameters = mutableMapOf<Int, CanvasParameters>()
    private val canvasScopes = mutableMapOf<Int, CoroutineScope>()
    private var lastVisibleTiles: List<TileSpecs>? = null
    private var lastZoomLevel: Int? = null

    fun getActiveTiles(id: Int): ActiveTiles = canvas.getValue(id).activeTiles.value

    fun resolveVisibleTiles(viewPort: ViewPort, zoomLevel: Int, mapProperties: MapProperties) {
        val visibleTiles = with(mapState) {
            getVisibleTilesForLevel(
                viewPort,
                zoomLevel,
                mapProperties.outsideTiles,
                mapProperties.tileSize
            )
        }
        lastVisibleTiles = visibleTiles
        lastZoomLevel = zoomLevel
        canvas.forEach{ (_, canvasEngine) -> canvasEngine.renderTiles(visibleTiles, zoomLevel) }
    }

    fun refreshCanvas(currentParameters: List<CanvasParameters>) {
        val currentIds = currentParameters.map { it.id }.toSet()

        val keysToRemove = canvas.keys.filter { it !in currentIds }
        keysToRemove.forEach(::removeCanvas)

        currentParameters.forEach { parameter ->
            val previous = canvasParameters[parameter.id]
            if (previous == null || previous.requiresNewEngine(parameter)) {
                removeCanvas(parameter.id)
                val engineJob = SupervisorJob(coroutineScope.coroutineContext[Job])
                val engineScope = CoroutineScope(coroutineScope.coroutineContext + engineJob)
                val engine = if (parameter is RasterCanvasParameters) {
                    RasterCanvasEngine(
                        parameter.maxCacheTiles,
                        parameter.tileSource,
                        engineScope,
                    )
                } else if (parameter is VectorCanvasParameters) {
                    VectorCanvasEngine(
                        parameter.maxCacheTiles,
                        parameter.tileSource,
                        engineScope,
                        parameter.style,
                    )
                } else {
                    error("Unsupported canvas parameters: ${parameter::class.simpleName}")
                }
                canvas[parameter.id] = engine
                canvasParameters[parameter.id] = parameter
                canvasScopes[parameter.id] = engineScope
                val visibleTiles = lastVisibleTiles
                val zoomLevel = lastZoomLevel
                if (visibleTiles != null && zoomLevel != null) {
                    engine.renderTiles(visibleTiles, zoomLevel)
                }
            }
        }
    }

    private fun removeCanvas(id: Int) {
        canvas.remove(id)
        canvasParameters.remove(id)
        canvasScopes.remove(id)?.cancel()
    }

    private fun Density.getVisibleTilesForLevel(
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

    private fun Density.getXYTile(position: Offset, zoomLevel: Int, tileDimension: TileDimension): Pair<Int, Int> = Pair(
        (position.x / tileDimension.width.toPx() * (1 shl zoomLevel)).toIntFloor(),
        (position.y / tileDimension.height.toPx() * (1 shl zoomLevel)).toIntFloor()
    )
}

private fun CanvasParameters.requiresNewEngine(current: CanvasParameters): Boolean = when {
    this::class != current::class -> true
    maxCacheTiles != current.maxCacheTiles -> true
    this is VectorCanvasParameters && current is VectorCanvasParameters -> style != current.style
    else -> false
}
