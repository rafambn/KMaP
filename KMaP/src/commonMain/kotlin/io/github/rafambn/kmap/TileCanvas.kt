package io.github.rafambn.kmap

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import io.github.rafambn.kmap.gestures.detectMapGestures
import kotlin.random.Random

@Composable
internal fun TileCanvas(
    modifier: Modifier,
//    tilesToRender: List<Tile>,
    tileSize: Float
) {
    val subSample = remember { mutableStateOf(0F) }
    val flingSpec = rememberSplineBasedDecay<Offset>()


    val listTiles = mutableListOf<Tile>()

    for (i in 0..24) {
        val row = i % 5
        val collumn = i / 5 % 5
        listTiles.add(Tile(0, row, collumn))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(true) {
                detectMapGestures(
                    onTap = { offset -> println("onTap $offset")},
                    onDoubleTap = { offset ->  println("onDoubleTap $offset")},
                    onTwoFingersTap = { offset -> println("onTwoFingersTap $offset")},
                    onLongPress = { offset -> println("onLongPress $offset") },
                    onTapLongPress = { offset ->  println("onTapLongPress $offset")},
                    onTapSwipe = { offset -> println("onTapSwipe $offset") },
                    onGesture = { centroid, pan, zoom, rotation -> println("onGesture $centroid $pan $zoom $rotation")},
                    onDrag = { dragAmount -> println("onDrag $dragAmount") },
                    onDragStart = { offset ->  println("onDragStart $offset")},
                    onDragEnd = { println("onDragEnd") },
                    onFling = { velocity ->  println("onFling $velocity")},
                    onFlingZoom = { velocity -> println("onFlingZoom $velocity") },
                    onFlingRotation = { velocity ->  println("onFlingRotation $velocity")},
                    onHover = { offset -> },
                    onScroll = { offset ->  }
                )
            }
    ) {
        withTransform({
//            rotate(
//                )
//            translate(left = -zoomPRState.scrollX, top = -zoomPRState.scrollY)
//            scale(1F, 2.5F, Offset.Zero)
        }) {

            drawIntoCanvas {
                for (tile in listTiles) {
                    it.drawRect(tileSize * tile.col, tileSize * tile.row, tileSize * tile.col + tileSize, tileSize * tile.row + tileSize, Paint().apply {
                        color = generateRandomColor()
                        isAntiAlias = false
                    })
                }
            }
        }
    }
}

fun generateRandomColor(): Color {
    val r = Random.nextFloat()
    val g = Random.nextFloat()
    val b = Random.nextFloat()
    return Color(r, g, b)
}
