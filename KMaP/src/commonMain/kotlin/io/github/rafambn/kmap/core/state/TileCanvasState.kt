package io.github.rafambn.kmap.core.state

import androidx.compose.ui.graphics.ImageBitmap
import io.github.rafambn.kmap.config.border.OutsideTilesType
import io.github.rafambn.kmap.config.characteristics.MapCoordinatesRange
import io.github.rafambn.kmap.core.TileLayers
import io.github.rafambn.kmap.model.BoundingBox
import io.github.rafambn.kmap.model.Tile
import io.github.rafambn.kmap.model.TileSpecs
import io.github.rafambn.kmap.utils.loopInZoom
import io.github.rafambn.kmap.utils.offsets.CanvasPosition
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.concurrent.Volatile
import kotlin.math.floor
import kotlin.math.pow
import kotlin.reflect.KFunction0

class TileCanvasState(
    private val updateState: KFunction0<Unit>,
    startZoom: Int
) {
    companion object {
        private const val MAX_RENDERED_TILES = 100
        private const val MAX_TRIES = 2
    }

    @Volatile
    var tileLayers = TileLayers(startZoom)
    private val renderedTiles = mutableListOf<Tile>()
    private val tilesToProcessChannel = Channel<List<TileSpecs>>(capacity = Channel.UNLIMITED)
    private val workerResultSuccessChannel = Channel<Tile>(capacity = Channel.UNLIMITED)
    private val workerResultFailedChannel = Channel<TileSpecs>(capacity = Channel.UNLIMITED)
    private val client = HttpClient()

    init {
        CoroutineScope(Dispatchers.Default).tilesKernel(tilesToProcessChannel, workerResultSuccessChannel, workerResultFailedChannel)
    }

    internal fun onStateChange(
        viewPort: BoundingBox,
        zoomLevel: Int,
        coordinatesRange: MapCoordinatesRange,
        outsideTiles: OutsideTilesType,
    ) {
        val nextVisibleTiles = getVisibleTilesForLevel(
            viewPort,
            zoomLevel,
            outsideTiles,
            coordinatesRange
        )
        val tilesToRender = mutableListOf<TileSpecs>()
        val newFrontLayer = nextVisibleTiles.map { tile ->
            renderedTiles.toList().find { tile == it } ?: run {
                tilesToRender.add(tile.toTileSpecs())
                tile
            }
        }
        if (zoomLevel == tileLayers.frontLayerLevel) {
            tileLayers.frontLayer = newFrontLayer
        } else {
            tileLayers.changeLayer(zoomLevel)
            tileLayers.frontLayer = newFrontLayer
        }
        tilesToProcessChannel.trySend(tilesToRender)
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
                    tilesToProcess.forEach { tileSpecs ->
                        if (!specsBeingProcessed.contains(tileSpecs)) {
                            specsBeingProcessed.add(tileSpecs)
                            worker(tileSpecs, tilesProcessSuccessResult, tilesProcessFailedResult)
                        }
                    }
                }
                tilesProcessSuccessResult.onReceive { tile ->
                    specsBeingProcessed.remove(tile.toTileSpecs())
                    renderedTiles.add(tile)
                    if (renderedTiles.size > MAX_RENDERED_TILES)
                        renderedTiles.removeAt(0)
                    if (tile.zoom == tileLayers.frontLayerLevel) {
                        tileLayers.frontLayer.find { it == tile }?.let { it.imageBitmap = tile.imageBitmap }
                        updateState.invoke()
                    }
                    if (tile.zoom == tileLayers.backLayerLevel) {
                        tileLayers.backLayer.find { it == tile }?.let { it.imageBitmap = tile.imageBitmap }
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
        println("${tileToProcess.zoom} -- ${tileToProcess.col} -- ${tileToProcess.row}")
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
    ): List<Tile> {
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

        val visibleTileSpecs = mutableListOf<Tile>()
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
                    visibleTileSpecs.add(Tile(zoomLevel, xTile, yTile, null))
                }
        } else {
            for (x in horizontalTileIntRange)
                for (y in verticalTileIntRange)
                    visibleTileSpecs.add(Tile(zoomLevel, x, y, null))
        }
        return visibleTileSpecs
    }

    private fun getXYTile(position: CanvasPosition, zoomLevel: Int, mapSize: MapCoordinatesRange): Pair<Int, Int> {
        return Pair(
            floor((position.horizontal - mapSize.longitute.getMin()) / mapSize.longitute.span * (1 shl zoomLevel)).toInt(),
            floor((-position.vertical + mapSize.latitude.getMax()) / mapSize.latitude.span * (1 shl zoomLevel)).toInt()
        )
    }
}