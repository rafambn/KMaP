package io.github.rafambn.kmap.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.rafambn.kmap.garbage.CharPath
import io.github.rafambn.kmap.states.MapState
import io.github.rafambn.kmap.states.TileCanvasState
import io.github.rafambn.kmap.states.rememberTileCanvasState
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    mapState: MapState
) {
    val tileCanvasState = rememberTileCanvasState()

    remember(key1 = mapState.mapPosition) {
        tileCanvasState.onPositionChange(
            mapState.mapPosition,
            mapState.zoomLevel,
            mapState.mapProperties.maxMapZoom,
            mapState.magnifierScale,
            mapState.angleDegrees,
            mapState.canvasSize,
            mapState.mapSize,
            mapState.mapProperties.outsideTiles
        )
    }

    val charPath = CharPath()
    val charWidth = 15f

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            rotate(
                degrees = mapState.angleDegrees, pivot = mapState.topLeftCanvas
            )
            scale(scale = mapState.magnifierScale, mapState.topLeftCanvas)
            translate(
                left = mapState.topLeftCanvas.x,
                top = mapState.topLeftCanvas.y
            )
        }) {
            drawIntoCanvas {
                for (tile in tileCanvasState.visibleTilesList.toList()) {
                    if (tile.zoom == mapState.zoomLevel) {
                        it.drawImage(tile.imageBitmap, Offset(
                            TileCanvasState.TILE_SIZE * tile.row,
                            TileCanvasState.TILE_SIZE * tile.col
                        ), Paint().apply {
                            color = generateRandomColor(tile.row, tile.col)
                            isAntiAlias = false   //TODO check performance when true
                        })

                        //TODO later remove this char
                        val pathString = "${
                            (tile.col + 2F.pow(mapState.zoomLevel).rem(2F.pow(mapState.zoomLevel))).toInt()
                        } - ${tile.row} - ${mapState.zoom.toInt()}"
                        var xOffset = 0f
                        it.save()
                        it.translate(
                            TileCanvasState.TILE_SIZE * tile.row + 80F,
                            TileCanvasState.TILE_SIZE * tile.col + 160F
                        )
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
}

fun generateRandomColor(row: Int, collumn: Int): Color {
    return if ((row + collumn) % 2 == 0)
        Color.Green
    else
        Color.Cyan
}