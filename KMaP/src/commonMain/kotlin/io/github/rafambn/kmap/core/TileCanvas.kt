package io.github.rafambn.kmap.core

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import io.github.rafambn.kmap.core.state.MapState
import io.github.rafambn.kmap.core.state.TileCanvasState
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileCanvasStateModel
import io.github.rafambn.kmap.utils.offsets.CanvasDrawReference
import io.github.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    getTile: suspend (zoom: Int, row: Int, column: Int) -> Tile
) {
    val canvasState = remember { TileCanvasState(getTile) }
    val tileCanvasStateModel = MapState.canvasSharedState.collectAsState(
            TileCanvasStateModel(
                Offset.Zero,
                0F,
                1F,
                CanvasDrawReference(0.0, 0.0),
                0,
                emptyList(),
                0
            )
        ).value
    canvasState.onStateChange(tileCanvasStateModel.visibleTileSpecs, tileCanvasStateModel.zoomLevel)
    val tileLayers = canvasState.tileLayersStateFlow.collectAsState().value
    Canvas(
        modifier = Modifier
    ) {
        withTransform({
            scale(tileCanvasStateModel.magnifierScale)
            rotate(tileCanvasStateModel.rotation)
            translate(tileCanvasStateModel.translation.x, tileCanvasStateModel.translation.y)
        }) {
            drawIntoCanvas { canvas ->
                val adjustedTileSize = 2F.pow(tileLayers.frontLayerLevel - tileLayers.backLayerLevel)
                for (tile in tileLayers.backLayer.toList()) {
                    tile.imageBitmap?.let {
                        canvas.drawImageRect(image = it,
                            dstOffset = IntOffset(
                                (tileCanvasStateModel.tileSize * tile.row * adjustedTileSize + tileCanvasStateModel.positionOffset.horizontal).dp.toPx()
                                    .toIntFloor(),
                                (tileCanvasStateModel.tileSize * tile.col * adjustedTileSize + tileCanvasStateModel.positionOffset.vertical).dp.toPx()
                                    .toIntFloor()
                            ),
                            dstSize = IntSize(
                                (tileCanvasStateModel.tileSize.dp.toPx() * adjustedTileSize).toIntFloor(),
                                (tileCanvasStateModel.tileSize.dp.toPx() * adjustedTileSize).toIntFloor()
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
                                (tileCanvasStateModel.tileSize * tile.row + tileCanvasStateModel.positionOffset.horizontal).dp.toPx()
                                    .toIntFloor(),
                                (tileCanvasStateModel.tileSize * tile.col + tileCanvasStateModel.positionOffset.vertical).dp.toPx().toIntFloor()
                            ),
                            dstSize = IntSize(
                                tileCanvasStateModel.tileSize.dp.toPx().toIntFloor(),
                                tileCanvasStateModel.tileSize.dp.toPx().toIntFloor()
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