package io.github.rafambn.kmap

import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.utils.loopInZoom
import io.github.rafambn.kmap.utils.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random
import kotlin.reflect.KFunction0

class TileCanvasState(
    private val updateState: KFunction0<Unit>
) {
    companion object {
        internal const val TILE_SIZE = 256 //TODO move to mapProperties
        private const val MAX_RENDERED_TILES = 100
        private const val MAX_TRIES = 2
    }

    var tileLayers = TileLayers()
    private val renderedTiles = mutableListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<Tile>(capacity = Channel.UNLIMITED)
    private val workerResultFailedChannel = Channel<TileSpecs>(capacity = Channel.UNLIMITED)
    private val client = HttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultSuccessChannel, workerResultFailedChannel)
    }

    internal fun onStateChange(
        screenState: ScreenState
    ) {
        val dd = getVisibleTilesForLevel(
            screenState.viewPort,
            screenState.zoomLevel,
            screenState.outsideTiles,
            screenState.coordinatesRange
        )
        tilesToProcessChannel.trySend(dd)
    }

    private fun CoroutineScope.tilesKernel(
        tilesToProcess: ReceiveChannel<List<TileSpecs>>,
        tilesProcessSuccessResult: Channel<Tile>,
        tilesProcessFailedResult: Channel<TileSpecs>
    ) = launch(Dispatchers.Default) {
        val specsBeingProcessed = mutableSetOf<TileSpecs>()

        while (isActive) {
            select {
                tilesToProcess.onReceive { tilesToProcess ->
                    val newTilesList = mutableListOf<Tile>()
                    tilesToProcess.forEach { tileSpecs ->
                        renderedTiles.find { it.equals(tileSpecs) }?.let {
                            newTilesList.add(it)
                            return@forEach
                        }
                        if (!specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessSuccessResult, tilesProcessFailedResult)
                        }
                    }
                    if (tilesToProcess[0].zoom == tileLayers.frontLayerLevel) {
                        if (newTilesList != tileLayers.frontLayer) {
                            tileLayers.frontLayer = newTilesList
                            updateState.invoke()
                        }
                    } else {
                        tileLayers.changeLayer()
                        tileLayers.frontLayer = newTilesList
                        tileLayers.frontLayerLevel = tilesToProcess[0].zoom
                        if (tilesToProcess[0].zoom - 1 < 0) {
                            tileLayers.backLayerEnable = false
                        } else {
                            tileLayers.backLayerEnable = true
                            tileLayers.backLayerLevel = tilesToProcess[0].zoom - 1
                        }
                    }
                }
                tilesProcessSuccessResult.onReceive { tile ->
                    specsBeingProcessed.find { it.equals(tile) }?.let {
                        specsBeingProcessed.remove(it)
                    }
                    renderedTiles.add(tile)
                    if (renderedTiles.size > MAX_RENDERED_TILES)
                        renderedTiles.removeAt(0)
                    if (tile.zoom == tileLayers.frontLayerLevel) {
                        tileLayers.frontLayer.add(tile)
                        updateState.invoke()
                    }
                }
                tilesProcessFailedResult.onReceive { tileSpecs ->
                    tileSpecs.takeIf { it.numberOfTries < MAX_TRIES }?.let {
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
//        println("${tileToProcess.zoom} -- ${tileToProcess.col} -- ${tileToProcess.row}")
        try {
            val byteArray = client.get(
                "https://tile.openstreetmap.org/${tileToProcess.zoom}/${(tileToProcess.row).loopInZoom(tileToProcess.zoom)}/${
                    (tileToProcess.col).loopInZoom(
                        tileToProcess.zoom
                    )
                }.png"
            ) {
                header("User-Agent", "my.app.test5")
            }.readBytes()
            imageBitmap = byteArray.toImageBitmap()
            tilesProcessSuccessResult.send(Tile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col, imageBitmap))
        } catch (ex: Exception) {
            if (tileToProcess.numberOfTries < MAX_TRIES)
                tilesProcessFailedResult.send(tileToProcess)
            else
                println("Failed to process tile after $MAX_TRIES attempts: $ex")
        }
    }

    private fun getVisibleTilesForLevel(
        viewPort: BoundingBox,
        zoomLevel: Int,
        outsideTilesType: OutsideTilesType,
        coordinatesRange: MapCoordinatesRange
    ): List<TileSpecs> {
        val topLeftTile = getXYTile(
            viewPort.topLeft,
            zoomLevel,
            coordinatesRange
        )
        val topRightTile = getXYTile(
            viewPort.topRight,
            zoomLevel,
            coordinatesRange
        )
        val bottomRightTile = getXYTile(
            viewPort.bottomRight,
            zoomLevel,
            coordinatesRange
        )
        val bottomLeftTile = getXYTile(
            viewPort.bottomLeft,
            zoomLevel,
            coordinatesRange
        )
        println("${viewPort.topLeft} - ${viewPort.topRight} - ${viewPort.bottomRight} - ${viewPort.bottomLeft}")
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
        if (outsideTilesType == OutsideTilesType.NONE) {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange) {
                    var xTile: Int
                    if (x < 0 || x > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        xTile = x
                    var yTile: Int
                    if (y < 0 || y > 2F.pow(zoomLevel) - 1)
                        continue
                    else
                        yTile = y
                    visibleTileSpecs.add(TileSpecs(zoomLevel, xTile, yTile))
                }
        } else {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange)
                    visibleTileSpecs.add(TileSpecs(zoomLevel, x, y))
        }
        return visibleTileSpecs
    }

    private fun getXYTile(position: Position, zoomLevel: Int, mapSize: MapCoordinatesRange): Pair<Int, Int> {
        return Pair(
            floor((position.horizontal - mapSize.longitute.getMin()) / mapSize.longitute.span * (1 shl zoomLevel)).toInt(),
            floor((-position.vertical + mapSize.latitude.getMax()) / mapSize.latitude.span * (1 shl zoomLevel)).toInt()
        )
    }
}