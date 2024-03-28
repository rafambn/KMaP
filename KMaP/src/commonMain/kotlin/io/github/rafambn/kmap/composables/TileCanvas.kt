package io.github.rafambn.kmap.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.rafambn.kmap.model.ScreenState
import io.github.rafambn.kmap.states.MapState
import io.github.rafambn.kmap.states.TileCanvasState
import io.github.rafambn.kmap.states.rememberTileCanvasState

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    mapState: MapState
) {
    val tileCanvasState = rememberTileCanvasState()

    remember(key1 = mapState.mapPosition) {
        tileCanvasState.onPositionChange(
            ScreenState(
                mapState.viewPort,
                mapState.zoomLevel,
                mapState.mapProperties.mapCoordinatesRange
            )
        )
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            transform(mapState.matrix)
        }) {
            drawIntoCanvas { canvas ->
                for (tile in tileCanvasState.visibleTilesList.toList()) {
                    if (tile.zoom == mapState.zoomLevel) {
                        tile.imageBitmap?.let {
                            canvas.drawImage(tile.imageBitmap, Offset(
                                (TileCanvasState.TILE_SIZE * tile.row + mapState.positionOffset.horizontal).toFloat(),
                                (TileCanvasState.TILE_SIZE * tile.col + mapState.positionOffset.vertical).toFloat()
                            ), Paint().apply {
                                isAntiAlias = true
                                filterQuality = FilterQuality.High
                            })
                        }
                    }
                }
            }
        }
    }
}