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

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    cameraState: CameraState,
    mapProperties: MapProperties
) {

    val tempSize = cameraState._tileSize.value / 3
    val listTiles = mutableListOf<Tile>()

    for (i in 0..8) {
        val row = i % 3
        val collumn = i / 3 % 3
        listTiles.add(Tile(0, row, collumn))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        withTransform({
            rotate(
                degrees = cameraState._angleDegres.value,
                pivot = Offset.Zero
            )
            translate(left = cameraState._rawPosition.value.x, top = cameraState._rawPosition.value.y)
            scale(scale = cameraState._zoom.value, Offset.Zero)
        }) {
            drawIntoCanvas {
                for (tile in listTiles) {
                    it.drawRect(Rect(Offset(tempSize * tile.col, tempSize * tile.row), Size(tempSize, tempSize)), Paint().apply {
                        color = generateRandomColor(tile.row, tile.col)
                        isAntiAlias = false
                    })
                }
            }
        }
    }
}

fun generateRandomColor(row: Int, collumn: Int): Color {
    return if (row == 1 && collumn == 1)
        Color.Green
    else
        Color.White
//    val r = Random.nextFloat()
//    val g = Random.nextFloat()
//    val b = Random.nextFloat()
//    return Color(r, g, b)
}
