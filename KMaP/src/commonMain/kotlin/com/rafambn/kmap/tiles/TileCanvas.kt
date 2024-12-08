package com.rafambn.kmap.tiles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.BoundingBox
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    cameraState: CameraState,
    mapProperties: MapProperties,
    positionOffset: CanvasDrawReference,
    boundingBox: BoundingBox,
    maxCacheTiles: Int,
) {
    val zoomLevel = cameraState.zoom.toIntFloor()
    val magnifierScale = cameraState.zoom - zoomLevel + 1F
    val tileSize = mapProperties.tileSize
    val rotationDegrees = cameraState.angleDegrees.toFloat()
    val translation = cameraState.canvasSize.asOffset() / 2F
    val visibleTiles = TileFinder().getVisibleTilesForLevel(
        boundingBox,
        zoomLevel,
        mapProperties.outsideTiles,
        mapProperties.mapCoordinatesRange
    )
    val coroutineScope = rememberCoroutineScope()
    var tileLayers = remember { TileLayers() }
    val canvasState = remember { TileRenderer(getTile, maxCacheTiles, coroutineScope) }

    val renderedTilesCache = canvasState.renderedTilesFlow.collectAsState()
    if (zoomLevel != tileLayers.frontLayer.level)
        tileLayers.changeLayer(zoomLevel)

    val newFrontLayer = mutableListOf<Tile>()
    val tilesToRender = mutableListOf<TileSpecs>()
    visibleTiles.forEach { tileSpecs ->
        val foundInFrontLayer = tileLayers.frontLayer.tiles.find { it == tileSpecs }
        val foundInRenderedTiles = renderedTilesCache.value.find {
            it == TileSpecs(
                tileSpecs.zoom,
                tileSpecs.row.loopInZoom(tileSpecs.zoom),
                tileSpecs.col.loopInZoom(tileSpecs.zoom)
            )
        }

        when {
            foundInFrontLayer != null -> {
                newFrontLayer.add(foundInFrontLayer)
            }

            foundInRenderedTiles != null -> {
                newFrontLayer.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, foundInRenderedTiles.imageBitmap))
            }

            else -> {
                newFrontLayer.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, null))
                tilesToRender.add(tileSpecs)
            }
        }
    }
    tileLayers.updateFrontLayerTiles(newFrontLayer)
    coroutineScope.launch { canvasState.renderTiles(tilesToRender) }
    renderedTilesCache.value.forEach {
        tileLayers.insertNewTileBitmap(it)
    }
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        withTransform({
            scale(magnifierScale)
            rotate(rotationDegrees)
            translate(translation.x, translation.y)
        }) {
            drawIntoCanvas { canvas ->
                val adjustedTileSize = 2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level)
                for (tile in tileLayers.backLayer.tiles.toList()) {
                    tile.imageBitmap?.let {
                        canvas.drawImageRect(image = it,
                            dstOffset = IntOffset(
                                (tileSize * tile.row * adjustedTileSize + positionOffset.horizontal).dp.toPx().toIntFloor(),
                                (tileSize * tile.col * adjustedTileSize + positionOffset.vertical).dp.toPx().toIntFloor()
                            ),
                            dstSize = IntSize(
                                (tileSize.dp.toPx() * adjustedTileSize).toIntFloor(),
                                (tileSize.dp.toPx() * adjustedTileSize).toIntFloor()
                            ),
                            paint = Paint().apply {
                                isAntiAlias = false
                                filterQuality = FilterQuality.High
                            }
                        )
                    }
                }
                for (tile in tileLayers.frontLayer.tiles.toList()) {
                    tile.imageBitmap?.let {
                        canvas.drawImageRect(image = it,
                            dstOffset = IntOffset(
                                (tileSize * tile.row + positionOffset.horizontal).dp.toPx().toIntFloor(),
                                (tileSize * tile.col + positionOffset.vertical).dp.toPx().toIntFloor()
                            ),
                            dstSize = IntSize(
                                tileSize.dp.toPx().toIntFloor(),
                                tileSize.dp.toPx().toIntFloor()
                            ),
                            paint = Paint().apply {
                                isAntiAlias = false
                                filterQuality = FilterQuality.High
                            }
                        )
                    }
                }
            }
        }
    }
}