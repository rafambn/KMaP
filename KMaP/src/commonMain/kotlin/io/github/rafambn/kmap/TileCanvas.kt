package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    translation: Offset,
    rotation: Float,
    magnifierScale: Float,
    visibleTilesList: List<Tile>,
    positionOffset: Position,
    mapState: Boolean
) {
    remember { mapState }
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            scale(magnifierScale)
            rotate(rotation)
            translate(translation.x, translation.y)
        }) {
            drawIntoCanvas { canvas ->
                for (tile in visibleTilesList) {
                    canvas.drawImageRect(image = tile.imageBitmap,
                        dstOffset = IntOffset(
                            floor((TileCanvasState.TILE_SIZE * tile.row + positionOffset.horizontal).dp.toPx()).toInt(),
                            floor((TileCanvasState.TILE_SIZE * tile.col + positionOffset.vertical).dp.toPx()).toInt()
                        ),
                        dstSize = IntSize(
                            TileCanvasState.TILE_SIZE.dp.toPx().toInt(),
                            TileCanvasState.TILE_SIZE.dp.toPx().toInt()
                        ),
                        paint = Paint().apply {
                            isAntiAlias = false
                            filterQuality = FilterQuality.High
                        }
                    )
                }
            }
        }
        drawIntoCanvas { //TODO remove later
            drawCircle(Color.Red, radius = 4F)
        }
    }
}