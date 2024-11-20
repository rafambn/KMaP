package com.rafambn.kmap.core

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.state.TileCanvasState
import com.rafambn.kmap.model.TileRenderResult
import com.rafambn.kmap.model.TileSpecs
import com.rafambn.kmap.utils.offsets.CanvasDrawReference
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    getTile: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
    magnifierScale: Float,
    rotationDegrees: Float,
    translation: Offset,
    tileSize: Int,
    positionOffset: CanvasDrawReference,
    zoomLevel: Int,
    visibleTiles: List<TileSpecs>,
    maxTries: Int = 2,
    maxCacheTiles: Int
) {
    val canvasState = remember { TileCanvasState(getTile, maxTries, maxCacheTiles) }
    var visibleTilesTracker by remember { mutableStateOf<List<TileSpecs>>(emptyList()) }
    if (visibleTilesTracker != visibleTiles) {
        visibleTilesTracker = visibleTiles
        canvasState.onStateChange(visibleTilesTracker, zoomLevel)
    }
    val tileLayers = canvasState.tileLayersStateFlow.collectAsState().value
    Canvas(
        modifier = Modifier
    ) {
        withTransform({
            scale(magnifierScale)
            rotate(rotationDegrees)
            translate(translation.x, translation.y)
        }) {
            drawIntoCanvas { canvas ->
                val adjustedTileSize = 2F.pow(tileLayers.frontLayerLevel - tileLayers.backLayerLevel)
                for (tile in tileLayers.backLayer.toList()) {
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
                for (tile in tileLayers.frontLayer.toList()) {
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