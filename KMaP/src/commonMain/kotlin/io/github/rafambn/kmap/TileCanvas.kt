package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.rafambn.kmap.states.MapState
import io.github.rafambn.kmap.states.rememberTileState
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    mapState: MapState
) {
    val tileCanvasState = rememberTileState()

    remember(key1 = mapState.rawPosition) {
        val centerTile = getXYTile(
            mapState.rawPosition.x.toDouble()/(2F.pow(mapState.zoomLevel - 1) * mapState.magnifierScale),//TODO rotate raw
            mapState.rawPosition.y.toDouble()/(2F.pow(mapState.zoomLevel - 1) * mapState.magnifierScale),
            mapState.zoomLevel,
            mapState.tileMapSize.x.toDouble(),
            mapState.tileMapSize.y.toDouble()
        )
        tileCanvasState.addTile(Tile(mapState.zoomLevel, centerTile.second, centerTile.first))
    }

    val charPath = CharPath()
    val charWidth = 15f

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            rotate(
                degrees = mapState.angleDegrees, pivot = mapState.mapViewCenter
            )
            scale(scale = mapState.magnifierScale, mapState.mapViewCenter)
            translate(
                left = mapState.mapViewCenter.x,
                top = mapState.mapViewCenter.y
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
                    val pathString = "${tile.col} - ${tile.row} - ${mapState.zoom.toInt()}"
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
