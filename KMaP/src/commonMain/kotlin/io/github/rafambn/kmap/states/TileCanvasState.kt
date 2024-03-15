package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.degreesToRadian
import io.github.rafambn.kmap.enums.OutsideTilesType
import io.github.rafambn.kmap.garbage.KtorClient
import io.github.rafambn.kmap.model.Position
import io.github.rafambn.kmap.model.ScreenState
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileSpecs
import io.github.rafambn.kmap.rotateVector
import io.github.rafambn.kmap.toImageBitmap
import io.github.rafambn.kmap.toMapReference
import io.github.rafambn.kmap.toOffset
import io.github.rafambn.kmap.toPosition
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.math.floor
import kotlin.math.pow

class TileCanvasState {
    val visibleTilesList = mutableStateListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val screenStateChannel = Channel<ScreenState>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<Tile>(capacity = Channel.UNLIMITED)
    private val client = KtorClient.provideHttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultChannel)
        CoroutineScope(Dispatchers.Default).visibleTilesResolver(screenStateChannel, tilesToProcessChannel)
    }

    fun onPositionChange(
        screenState: ScreenState
    ) {
        screenStateChannel.trySend(screenState)
    }

    private fun CoroutineScope.visibleTilesResolver(
        screenStateChannel: ReceiveChannel<ScreenState>,
        tilesToProcess: SendChannel<List<TileSpecs>>
    ) = launch(Dispatchers.Default) {
        while (true) {
            val screenState = screenStateChannel.receive()
            val halfScreenScaled = screenState.viewSize / 2.0
            val topLeft = (-halfScreenScaled.rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val bottomRight = (halfScreenScaled.rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val topRight =
                (Position(halfScreenScaled.horizontal, -halfScreenScaled.vertical).rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val bottomLeft =
                (Position(-halfScreenScaled.horizontal, halfScreenScaled.vertical).rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)

            val maxHorizontal = maxOf(topLeft.horizontal, topRight.horizontal, bottomRight.horizontal, bottomLeft.horizontal)
            val minHorizontal = minOf(topLeft.horizontal, topRight.horizontal, bottomRight.horizontal, bottomLeft.horizontal)
            val maxVertical = maxOf(topLeft.vertical, topRight.vertical, bottomRight.vertical, bottomLeft.vertical)
            val minVertical = minOf(topLeft.vertical, topRight.vertical, bottomRight.vertical, bottomLeft.vertical)

            val topLeftTile = getXYTile(
                minHorizontal,
                minVertical,
                screenState.zoomLevel,
                screenState.mapSize.horizontal,
                screenState.mapSize.vertical
            )
            val bottomRightTile = getXYTile(
                maxHorizontal,
                maxVertical,
                screenState.zoomLevel,
                screenState.mapSize.horizontal,
                screenState.mapSize.vertical
            )
            val visibleTileSpecs = mutableListOf<TileSpecs>()
            for (x in topLeftTile.first..bottomRightTile.first)
                for (y in topLeftTile.second..bottomRightTile.second) {
                    var xTile: Int
                    if (screenState.outsideTiles.horizontal == OutsideTilesType.NONE)
                        if (x < 0 || x > 2F.pow(screenState.zoomLevel) - 1)
                            continue
                        else
                            xTile = x
                    else
                        xTile = loop(x, screenState.zoomLevel)
                    var yTile: Int
                    if (screenState.outsideTiles.vertical == OutsideTilesType.NONE)
                        if (y < 0 || y > 2F.pow(screenState.zoomLevel) - 1)
                            continue
                        else
                            yTile = y
                    else
                        yTile = loop(y, screenState.zoomLevel)
                    visibleTileSpecs.add(TileSpecs(screenState.zoomLevel, xTile, yTile))
                }
            tilesToProcess.send(visibleTileSpecs)
        }
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessResult: Channel<Tile>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableSetOf<TileSpecs>()
        val specsProcessed = mutableSetOf<TileSpecs>()

        while (true) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
                    for (tileSpecs in tilesToProcess) {
                        if (!specsProcessed.contains(tileSpecs) && !specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessResult)
                        }
                    }
                }
                tilesProcessResult.onReceive { tile ->
                    val tileSpecs = TileSpecs(tile.zoom, tile.row, tile.col)
                    specsBeingProcessed.remove(tileSpecs)
                    specsProcessed.add(tileSpecs)
                    visibleTilesList.add(tile)

                    if (tile.imageBitmap == null) {
                        tileSpecs.takeIf { it.numberOfTries < 2 }?.let {
                            it.numberOfTries++
                            worker(it, tilesProcessResult)
                        }
                    }
                }
            }
        }
    }


    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tileProcessResult: SendChannel<Tile>
    ) = launch(Dispatchers.Default) {
        val imageBitmap: ImageBitmap
        try {
            imageBitmap =
                client.get("https://tile.openstreetmap.org/${tileToProcess.zoom}/${tileToProcess.row}/${tileToProcess.col}.png") {
                    header("User-Agent", "my.app.test1")
                }.readBytes().toImageBitmap()
            tileProcessResult.send(Tile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, imageBitmap))
        } catch (ex: Exception) {
            tileProcessResult.send(Tile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, null))
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
        internal const val TILE_SIZE = 256.0
        internal const val MAP_SIZE = 1000000.0
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}