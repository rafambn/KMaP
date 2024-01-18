package io.github.rafambn.kmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.random.Random

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    tileSize: Float
) {

    val listTiles = mutableListOf<Tile>()

    for (i in 0..24) {
        val row = i % 5
        val collumn = i / 5 % 5
        listTiles.add(Tile(0, row, collumn))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        drawIntoCanvas {
            for (tile in listTiles) {
                it.drawRect(tileSize * tile.col, tileSize * tile.row, tileSize * tile.col + tileSize, tileSize * tile.row + tileSize, Paint().apply {
                    color = generateRandomColor(tile.row, tile.col)
                    isAntiAlias = false
                })
            }
        }
    }
}

fun generateRandomColor(row: Int, collumn: Int): Color {
    if (row == 2 && collumn == 2)
        return Color.Green
    else
        return Color.White
//    val r = Random.nextFloat()
//    val g = Random.nextFloat()
//    val b = Random.nextFloat()
//    return Color(r, g, b)
}
