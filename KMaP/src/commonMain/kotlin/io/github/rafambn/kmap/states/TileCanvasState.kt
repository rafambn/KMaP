package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.degreesToRadian
import io.github.rafambn.kmap.enums.OutsideTilesType
import io.github.rafambn.kmap.garbage.KtorClient
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileSpecs
import io.github.rafambn.kmap.rotateVector
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
import kotlin.math.pow

class TileCanvasState {
    var visibleTilesList = mutableStateListOf<Tile>()
    private val tileKernelChannel = Channel<TileSpecs>(capacity = Channel.UNLIMITED)
    private val client = KtorClient.provideHttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tileKernelChannel)
    }

    fun onZoomChange() {
//        visibleTilesList.clear()
    }

    fun onPositionChange(
        position: Offset,
        zoomLevel: Int,
        maxZoom: Float,
        magnifierScale: Float,
        angle: Float,
        viewSize: Offset,
        mapSize: Offset,
        outsideTiles: OutsideMapTiles
    ) {
        val halfScreenScaled = (viewSize / 2F) * 2F.pow(maxZoom - zoomLevel) / magnifierScale
        val topLeft = (-halfScreenScaled - position).rotateVector(-angle.degreesToRadian())
        val bottomRight = (halfScreenScaled - position).rotateVector(-angle.degreesToRadian())
        val topRight = (Offset(halfScreenScaled.x, -halfScreenScaled.y) - position).rotateVector(-angle.degreesToRadian())
        val bottomLeft = (Offset(-halfScreenScaled.x, halfScreenScaled.y) - position).rotateVector(-angle.degreesToRadian())
        //TODO fix problem with rotation
        val maxHorizontal = maxOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
        val minHorizontal = minOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
        val maxVertical = maxOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)
        val minVertical = minOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

        val topLeftTile = getXYTile(
            minHorizontal.toDouble(),
            minVertical.toDouble(),
            zoomLevel,
            mapSize.x.toDouble(),
            mapSize.y.toDouble()
        )
        val bottomRightTile = getXYTile(
            maxHorizontal.toDouble(),
            maxVertical.toDouble(),
            zoomLevel,
            mapSize.x.toDouble(),
            mapSize.y.toDouble()
        )
        val visibleTileSpecs = mutableListOf<TileSpecs>()
        for (x in topLeftTile.first..bottomRightTile.first)
            for (y in topLeftTile.second..bottomRightTile.second) {
                var xTile: Int
                if (outsideTiles.horizontal == OutsideTilesType.NONE)
                    if (x < 0 || x > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        xTile = x
                else
                    xTile = loop(x, zoomLevel)
                var yTile: Int
                if (outsideTiles.vertical == OutsideTilesType.NONE)
                    if (y < 0 || y > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        yTile = y
                else
                    yTile = loop(y, zoomLevel)
                visibleTileSpecs.add(TileSpecs(zoomLevel,xTile,yTile))
            }

        visibleTileSpecs.forEach {tilesSpecs ->
            visibleTilesList.find { it.zoom == zoomLevel && it.col == tilesSpecs.col && it.row == tilesSpecs.row } ?: run {
                tileKernelChannel.trySend(TileSpecs(zoomLevel, tilesSpecs.row, tilesSpecs.col))
            }
        }
    }


    private fun CoroutineScope.tilesKernel(
        tilesToProcess: Channel<TileSpecs>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableListOf<TileSpecs>()

        while (true) {
            select {
                tilesToProcess.onReceive { tileSpecs ->
                    specsBeingProcessed.find { it == tileSpecs } ?: run {
                        specsBeingProcessed.add(tileSpecs)
                        CoroutineScope(Dispatchers.Default).launch {
                            val imageBitmap: ImageBitmap
                            val byteArray: ByteArray
                            try {
                                println("${tileSpecs.zoom}/${tileSpecs.row}/${tileSpecs.col}")
                                byteArray =
                                    client.get("http://tile.openstreetmap.org/${tileSpecs.zoom}/${tileSpecs.row}/${tileSpecs.col}.png") {
                                        header("User-Agent", "my.app.test1")
                                    }.readBytes()
                                println(byteArray)
                                imageBitmap = byteArray.toImageBitmap()
                                visibleTilesList.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, imageBitmap))
                            } catch (ex: Exception) {
                                println(ex)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getXYTile(x: Double, y: Double, zoom: Int, width: Double, height: Double): Pair<Int, Int> {
        return Pair(floor(x / width * (1 shl zoom)).toInt(), floor(y / height * (1 shl zoom)).toInt())
    }

    fun loop(value: Int, zoom: Int): Int {
        val zoomFactor = 1 shl zoom
        return (value + zoomFactor) % zoomFactor
    }

    companion object {
        internal const val TILE_SIZE = 256F
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}