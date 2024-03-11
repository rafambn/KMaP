package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.degreesToRadian
import io.github.rafambn.kmap.enums.OutsideTilesType
import io.github.rafambn.kmap.garbage.KtorClient
import io.github.rafambn.kmap.model.ScreenState
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
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.math.floor
import kotlin.math.pow

class TileCanvasState {
    val visibleTilesList = mutableListOf<Tile>() //TODO make it a state
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val screenStateChannel = Channel<ScreenState>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<Tile>(capacity = Channel.UNLIMITED)
    private val client = KtorClient.provideHttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultChannel)
        CoroutineScope(Dispatchers.Default).visibleTilesResolver(screenStateChannel, tilesToProcessChannel)
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
        screenStateChannel.trySend(ScreenState(position, zoomLevel, maxZoom, magnifierScale, angle, viewSize, mapSize, outsideTiles))
    }

    private fun CoroutineScope.visibleTilesResolver(
        screenStateChannel: ReceiveChannel<ScreenState>,
        tilesToProcess: SendChannel<List<TileSpecs>>
    ) = launch(Dispatchers.Default) {
        while (true) {
            val screenState = screenStateChannel.receive()
            val halfScreenScaled = (screenState.viewSize / 2F) * 2F.pow(screenState.maxZoom - screenState.zoomLevel) / screenState.magnifierScale
            val topLeft = (-halfScreenScaled.rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val bottomRight = (halfScreenScaled.rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val topRight =
                (Offset(halfScreenScaled.x, -halfScreenScaled.y).rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)
            val bottomLeft =
                (Offset(-halfScreenScaled.x, halfScreenScaled.y).rotateVector(-screenState.angle.degreesToRadian()) - screenState.position)

            val maxHorizontal = maxOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
            val minHorizontal = minOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
            val maxVertical = maxOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)
            val minVertical = minOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

            val topLeftTile = getXYTile(
                minHorizontal.toDouble(),
                minVertical.toDouble(),
                screenState.zoomLevel,
                screenState.mapSize.x.toDouble(),
                screenState.mapSize.y.toDouble()
            )
            val bottomRightTile = getXYTile(
                maxHorizontal.toDouble(),
                maxVertical.toDouble(),
                screenState.zoomLevel,
                screenState.mapSize.x.toDouble(),
                screenState.mapSize.y.toDouble()
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
        val specsBeingProcessed = mutableListOf<TileSpecs>()

        while (true) {
            select<Unit> {
                tilesToProcess.onReceive { tilesToProcess ->
                    for (tileSpecs in tilesToProcess) {
                        visibleTilesList.find { it.zoom == tileSpecs.zoom && it.col == tileSpecs.col && it.row == tileSpecs.row }
                            ?: run {
                                specsBeingProcessed.find { it == tileSpecs } ?: run {
                                    specsBeingProcessed.add(tileSpecs)
                                    worker(tileSpecs, tilesProcessResult)
                                }
                            }
                    }
                }
                tilesProcessResult.onReceive { tile ->
                    tile.imageBitmap?.let {
                        specsBeingProcessed.remove(TileSpecs(tile.zoom, tile.row, tile.col))
                        visibleTilesList.add(tile)
                    } ?: run {
                        specsBeingProcessed.find { it.zoom == tile.zoom && it.col == tile.col && it.row == tile.row }?.let {
                            if (it.numberOfTries < 2) {
                                it.numberOfTries++
                                worker(it, tilesProcessResult)
                            } else {
                                specsBeingProcessed.remove(it)
                                visibleTilesList.add(tile)
                            }
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
            println("${tileToProcess.zoom}/${tileToProcess.row}/${tileToProcess.col}")
            imageBitmap =
                client.get("http://tile.openstreetmap.org/${tileToProcess.zoom}/${tileToProcess.row}/${tileToProcess.col}.png") {
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
        internal const val TILE_SIZE = 256F
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}