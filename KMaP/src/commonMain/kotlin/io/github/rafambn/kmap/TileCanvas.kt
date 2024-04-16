package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    mapState: Boolean,
    boundingBox: BoundingBox,
    zoomLevel: Int,
    coordinatesRange: MapCoordinatesRange,
    outsideTiles: OutsideTilesType,
    matrix: Matrix,
    positionOffset: Position,
    updateState: Unit
) {
    val tileCanvasState = rememberTileCanvasState(updateState)

    remember(key1 = mapState) {
        tileCanvasState.onStateChange(
            ScreenState(
                boundingBox,
                zoomLevel,
                coordinatesRange,
                outsideTiles
            )
        )
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            transform(matrix)
        }) {
            drawIntoCanvas { canvas ->
                for (tile in tileCanvasState.visibleTilesList.toList()) {
                    canvas.drawImage(tile.imageBitmap, Offset(
                        (TileCanvasState.TILE_SIZE * tile.row + positionOffset.horizontal).toFloat(),
                        (TileCanvasState.TILE_SIZE * tile.col + positionOffset.vertical).toFloat()
                    ), Paint().apply {
                        isAntiAlias = false
                        filterQuality = FilterQuality.High
                    })
                }
            }
        }
        drawIntoCanvas { //TODO remove later
            drawCircle(Color.Red, radius = 4F)
        }
    }
}