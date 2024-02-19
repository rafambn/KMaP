package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import io.github.rafambn.kmap.states.rememberTileState

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    angle: Float,
    position: Offset,
    zoom: Float,
    mapSize: IntSize
) {
    val tileCanvasState = rememberTileState()
    val charPath = CharPath()
    val charWidth = 15f

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            rotate(
                degrees = angle, pivot = position
            )
            scale(scale = zoom, position)
            translate(
                left = position.x + (mapSize.width - tileCanvasState.tileSize) / 2,
                top = position.y + (mapSize.height - tileCanvasState.tileSize) / 2
            )
        }) {
            drawIntoCanvas {
                for (tile in tileCanvasState.listTiles) {
                    it.drawRect(
                        Rect(
                            Offset(tileCanvasState.tileSize * tile.col, tileCanvasState.tileSize * tile.row),
                            Size(tileCanvasState.tileSize, tileCanvasState.tileSize)
                        ), Paint().apply {
                            color = generateRandomColor(tile.row, tile.col)
                            isAntiAlias = false
                        })


                    //TODO later remove this char
                    val pathString = "${tile.col} - ${tile.row}"
                    var xOffset = 0f
                    it.save()
                    it.translate(tileCanvasState.tileSize * tile.col + 80F, tileCanvasState.tileSize * tile.row + 160F)
                    for (char in pathString) {
                        val path = charPath.paths[char]
                        if (path != null) {
                            it.save()
                            it.translate(xOffset, -25f)  // Translate the canvas
                            it.drawPath(path, Paint().apply {
                                color = Color.Black
                                isAntiAlias = false
                            })
                            it.restore()
                            xOffset += charWidth
                        }
                    }
                    it.restore()
                }
            }
        }
    }
}

fun generateRandomColor(row: Int, collumn: Int): Color {
    return if ((row + collumn) % 2 == 0)
        Color.Green
    else
        Color.Cyan
}
