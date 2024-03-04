package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.KtorClient
import io.github.rafambn.kmap.tiles.Tile
import io.github.rafambn.kmap.tiles.TileSpecs
import io.github.rafambn.kmap.toImageBitmap
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.math.floor

class TileCanvasState {
    var visibleTilesList = mutableStateListOf<Tile>()
    private val tileKernelChannel = Channel<TileSpecs>(capacity = Channel.UNLIMITED)
    val client = KtorClient.provideHttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tileKernelChannel)
    }

    fun onZoomChange() {
        visibleTilesList.clear()
    }

    fun onPositionChange(position: Offset, zoomLevel: Int, magnifierScale: Float, angle: Float, viewSize: Offset, tileMapSize: Offset) {
        val tile = getXYTile(
            position.x.toDouble(),
            position.y.toDouble(),
            zoomLevel,
            tileMapSize.x.toDouble(),
            tileMapSize.y.toDouble()
        )

        visibleTilesList.find { it.zoom == zoomLevel && it.col == tile.second && it.row == tile.first } ?: run {
            tileKernelChannel.trySend(TileSpecs(zoomLevel, tile.first, tile.second))
        }

//        val halfScreenScaled = (viewSize / 2F) * magnifierScale / 2F.pow(zoomLevel-1)
//        println(halfScreenScaled)
//        val bottomRight = (position + halfScreenScaled).rotateVector(angle.degreesToRadian())
//        val bottomLeft = (position + Offset(-halfScreenScaled.x, halfScreenScaled.y).rotateVector(angle.degreesToRadian()))
//        val topRight = -bottomLeft
//        val topLeft = -bottomRight
//
//        val maxHorizontal = maxOf(bottomRight.x, bottomLeft.x, topRight.x, topLeft.x)
//        val minHorizontal = maxOf(bottomRight.x, bottomLeft.x, topRight.x, topLeft.x)
//        val maxVertical = maxOf(bottomRight.y, bottomLeft.y, topRight.y, topLeft.y)
//        val minVertical = maxOf(bottomRight.y, bottomLeft.y, topRight.y, topLeft.y)
//
//
//        getVisibleTiles(
//            Offset(maxHorizontal, maxVertical),
//            Offset(minHorizontal, minVertical),
//            zoomLevel,
//            tileMapSize
//        ).forEach { tile ->
//            visibleTilesList.find { it == tile } ?: run { visibleTilesList.add(tile) }
//        }
    }

//    private fun getVisibleTiles(bottomRight: Offset, topLeft: Offset, zoomLevel: Int, tileMapSize: Offset): List<Tile> {
//        val topLeftTile = getXYTile(
//            topLeft.x.toDouble(),
//            topLeft.y.toDouble(),
//            zoomLevel,
//            tileMapSize.x.toDouble(),
//            tileMapSize.y.toDouble()
//        )
//        val bottomRightTile = getXYTile(
//            bottomRight.x.toDouble(),
//            bottomRight.y.toDouble(),
//            zoomLevel,
//            tileMapSize.x.toDouble(),
//            tileMapSize.y.toDouble()
//        )
//
//
//        val tileSpecsList = mutableListOf<Tile>()
//        for (i in topLeftTile.first..bottomRightTile.first)
//            for (j in topLeftTile.second..bottomRightTile.second)
//                tileSpecsList.add(Tile(zoomLevel, i, j))
//        return tileSpecsList
//    }


    private fun CoroutineScope.tilesKernel(
        tilesToProcess: Channel<TileSpecs>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableListOf<TileSpecs>()

        while (true) {
            select {
                tilesToProcess.onReceive { tileSpecs ->
                    specsBeingProcessed.find { it == tileSpecs }?: run {
                        specsBeingProcessed.add(tileSpecs)
                        CoroutineScope(Dispatchers.Default).launch {
                            val imageBitmap: ImageBitmap
                            try {
                                println("teste")
                                imageBitmap =
                                    client.get("http://tile.openstreetmap.org/${tileSpecs.zoom}/${tileSpecs.row}/${tileSpecs.col}.png") {
                                        header("User-Agent", "my.app")
                                    }.readBytes().toImageBitmap()
                                visibleTilesList.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, imageBitmap))
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getXYTile(x: Double, y: Double, zoom: Int, width: Double, height: Double): Pair<Int, Int> {
        var xtile = floor(-x / width * (1 shl zoom)).toInt()
        var ytile = floor(-y / height * (1 shl zoom)).toInt()

        if (xtile < 0) {
            xtile = 0
        }
        if (xtile >= (1 shl zoom)) {
            xtile = (1 shl zoom) - 1
        }
        if (ytile < 0) {
            ytile = 0
        }
        if (ytile >= (1 shl zoom)) {
            ytile = (1 shl zoom) - 1
        }
        return Pair(xtile, ytile)
    }

    companion object {
        internal val tileSize = 256F
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}