package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.enums.OutsideTilesType
import io.github.rafambn.kmap.garbage.KtorClient
import io.github.rafambn.kmap.loopInRange
import io.github.rafambn.kmap.loopInZoom
import io.github.rafambn.kmap.model.Position
import io.github.rafambn.kmap.model.ScreenState
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileCore
import io.github.rafambn.kmap.model.TileSpecs
import io.github.rafambn.kmap.ranges.MapCoordinatesRange
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Image
import kotlin.math.floor
import kotlin.math.pow

class TileCanvasState {
    private val mutex = Mutex()
    var visibleTilesList = mutableStateListOf<Tile>()
    private val renderedTiles = mutableListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val screenStateChannel = Channel<ScreenState>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<Tile>(capacity = Channel.UNLIMITED)
    private val workerResultFailedChannel = Channel<TileSpecs>(capacity = Channel.UNLIMITED)
    private val client = KtorClient.provideHttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultSuccessChannel, workerResultFailedChannel)
        CoroutineScope(Dispatchers.Default).visibleTilesResolver(screenStateChannel, tilesToProcessChannel)
    }

    fun onStateChange(
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

            val topLeftTile = getXYTile(
                screenState.viewPort.topLeft,
                screenState.zoomLevel,
                screenState.coordinatesRange
            )
            val topRightTile = getXYTile(
                screenState.viewPort.topRight,
                screenState.zoomLevel,
                screenState.coordinatesRange
            )
            val bottomRightTile = getXYTile(
                screenState.viewPort.bottomRight,
                screenState.zoomLevel,
                screenState.coordinatesRange
            )
            val bottomLeftTile = getXYTile(
                screenState.viewPort.bottomLeft,
                screenState.zoomLevel,
                screenState.coordinatesRange
            )
            val horizontalTileIntRange =
                IntRange(
                    minOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first),
                    maxOf(topLeftTile.first, bottomRightTile.first, topRightTile.first, bottomLeftTile.first)
                )
            val verticalTileIntRange =
                IntRange(
                    minOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second),
                    maxOf(topLeftTile.second, bottomRightTile.second, topRightTile.second, bottomLeftTile.second)
                )

            val visibleTileSpecs = mutableListOf<TileSpecs>()
            if (screenState.outsideTiles == OutsideTilesType.NONE) {
                for (x in horizontalTileIntRange)
                    for (y in verticalTileIntRange) {
                        var xTile: Int
                        if (x < 0 || x > 2F.pow(screenState.zoomLevel) - 1)
                            continue
                        else
                            xTile = x
                        var yTile: Int
                        if (y < 0 || y > 2F.pow(screenState.zoomLevel) - 1)
                            continue
                        else
                            yTile = y
                        visibleTileSpecs.add(TileSpecs(screenState.zoomLevel, xTile, yTile))
                    }
            } else {
                for (x in horizontalTileIntRange)
                    for (y in verticalTileIntRange)
                        visibleTileSpecs.add(TileSpecs(screenState.zoomLevel, x, y))
            }
            tilesToProcess.send(visibleTileSpecs)
        }
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessSuccessResult: Channel<Tile>,
        tilesProcessFailedResult: Channel<TileSpecs>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableSetOf<TileSpecs>()

        while (true) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
                    visibleTilesList = mutableStateListOf()
                    tilesToProcess.forEach { tileSpecs ->
                        mutex.withLock {
                            renderedTiles.find { it.equals(tileSpecs) }?.let {
                                visibleTilesList.add(it)
                                return@forEach
                            }
                        }
                        if (!specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessSuccessResult, tilesProcessFailedResult)
                        }
                    }
                }
                tilesProcessSuccessResult.onReceive { tile ->
                    specsBeingProcessed.remove(tile as TileCore)
                    mutex.withLock {
                        renderedTiles.add(tile)
                        if (renderedTiles.size > 100)
                            renderedTiles.removeAt(0)
                    }
                    visibleTilesList.add(tile)
                }
                tilesProcessFailedResult.onReceive { tileSpecs ->
                    tileSpecs.takeIf { it.numberOfTries < 2 }?.let {
                        it.numberOfTries++
                        worker(it, tilesProcessSuccessResult, tilesProcessFailedResult)
                    }
                }
            }
        }
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessSuccessResult: SendChannel<Tile>,
        tilesProcessFailedResult: SendChannel<TileSpecs>
    ) = launch(Dispatchers.Default) {
        val imageBitmap: ImageBitmap
        val image: Image
        try {
            val byteArray = client.get(
                "https://tile.openstreetmap.org/${tileToProcess.zoom}/${(tileToProcess.row).loopInZoom(tileToProcess.zoom)}/${
                    (tileToProcess.col).loopInZoom(
                        tileToProcess.zoom
                    )
                }.png"
            ) {
                header("User-Agent", "my.app.test3")
            }.readBytes()
            imageBitmap = byteArray.toImageBitmap()
            image = Image.makeFromEncoded(byteArray)
            tilesProcessSuccessResult.send(Tile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, imageBitmap))
        } catch (ex: Exception) {
            println(ex)
            tilesProcessFailedResult.send(TileSpecs(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, tileToProcess.numberOfTries++))
        }
    }

    private fun getXYTile(position: Position, zoomLevel: Int, mapSize: MapCoordinatesRange): Pair<Int, Int> {
        return Pair(
            floor((position.horizontal - mapSize.longitute.getMin()) / mapSize.longitute.span * (1 shl zoomLevel)).toInt(),
            floor((-position.vertical + mapSize.latitude.getMax()) / mapSize.latitude.span * (1 shl zoomLevel)).toInt()
        )
    }

    companion object {
        internal const val TILE_SIZE = 256
    }
}

@Composable
inline fun rememberTileCanvasState(
    crossinline init: TileCanvasState.() -> Unit = {}
): TileCanvasState = remember {
    TileCanvasState().apply(init)
}