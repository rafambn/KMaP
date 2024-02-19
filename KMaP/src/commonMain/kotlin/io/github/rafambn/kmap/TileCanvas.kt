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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.rafambn.kmap.states.CameraState
import io.github.rafambn.kmap.states.MapProperties

@Composable
internal fun TileCanvas(
    modifier: Modifier, cameraState: CameraState, mapProperties: MapProperties
) {

    val tempSize = cameraState.tileSize
    val listTiles = mutableListOf<Tile>()

    for (i in 0..8) {
        val row = i % 3
        val column = i / 3 % 3
        listTiles.add(Tile(0, row, column))
    }
    val charPath = CharPath()
    val charWidth = 15f

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        withTransform({
            rotate(
                degrees = cameraState.angleDegres.value, pivot = cameraState.rawPosition.value
            )
            scale(scale = cameraState.zoom.value, cameraState.rawPosition.value)
            translate(
                left = cameraState.rawPosition.value.x + (cameraState.mapSize.width - tempSize) / 2,
                top = cameraState.rawPosition.value.y + (cameraState.mapSize.height - tempSize) / 2
            )
        }) {
            drawIntoCanvas {
                for (tile in listTiles) {
                    it.drawRect(Rect(Offset(tempSize * tile.col, tempSize * tile.row), Size(tempSize, tempSize)), Paint().apply {
                        color = generateRandomColor(tile.row, tile.col)
                        isAntiAlias = false
                    })


                    //TODO later remove this char
                    val pathString = "${tile.col} - ${tile.row}"
                    var xOffset = 0f
                    it.save()
                    it.translate(tempSize * tile.col + 80F, tempSize * tile.row + 160F)
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
    return if (row == 1 && collumn == 1) Color.Green
    else if (row == 2 && collumn == 1) Color.Cyan
    else Color.White
//    val r = Random.nextFloat()
//    val g = Random.nextFloat()
//    val b = Random.nextFloat()
//    return Color(r, g, b)
}
