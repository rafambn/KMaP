package com.rafambn.kmap.source.internal

import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.TileResult
import com.rafambn.kmap.source.TileSpecs
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select

/**
 * Loads and processes source coordinates already normalized by [CanvasEngine].
 * The engine owns repeated display copies; this renderer tracks only in-flight source tiles.
 * [dispatcher] runs both coordination and tile workers.
 */
class TileRenderer<T : Tile, R : Tile>(
    coroutineScope: CoroutineScope,
    private val getTile: suspend (zoom: Int, row: Int, column: Int) -> TileResult<T>,
    private val processTile: suspend (T) -> R,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val requiredTiles = Channel<List<TileSpecs>>(capacity = Channel.CONFLATED)
    private val completedTiles = Channel<TileResult<R>>(capacity = Channel.UNLIMITED)
    private val workerResultChannel = Channel<TileResult<R>>(capacity = Channel.UNLIMITED)

    /** Successes and failures for one consumer. Cancelled when the renderer stops. */
    val results: ReceiveChannel<TileResult<R>> = completedTiles

    init {
        coroutineScope.launch(dispatcher + SupervisorJob(coroutineScope.coroutineContext[Job])) {
            val tilesBeingProcessed = mutableSetOf<TileSpecs>()

            while (isActive) {
                select {
                    requiredTiles.onReceive { tilesToProcess ->
                        // A newer update may arrive after this receive resumes but before workers start.
                        val latestRequiredTiles = requiredTiles.tryReceive().getOrNull() ?: tilesToProcess
                        latestRequiredTiles.forEach { specs ->
                            if (tilesBeingProcessed.add(specs)) {
                                worker(specs, workerResultChannel)
                            }
                        }
                    }
                    workerResultChannel.onReceive { tileResult ->
                        val finishedSpecs = when (tileResult) {
                            is TileResult.Success -> tileResult.tile
                            is TileResult.Failure -> tileResult.specs
                        }
                        tilesBeingProcessed.remove(TileSpecs(finishedSpecs.zoom, finishedSpecs.row, finishedSpecs.col))
                        completedTiles.send(tileResult)
                    }
                }
            }
        }.invokeOnCompletion {
            requiredTiles.cancel()
            workerResultChannel.cancel()
            completedTiles.cancel()
        }
    }

    /**
     * Queues a copy of normalized source coordinates, replacing the previous pending update.
     * An empty list clears pending requests. In-flight work continues and publishes its result.
     * Calls after the renderer stops are ignored; retries require another update.
     */
    fun updateRequiredTiles(tiles: List<TileSpecs>) {
        requiredTiles.trySend(tiles.toList())
    }

    private fun CoroutineScope.worker(
        tileToProcess: TileSpecs,
        tilesProcessResult: SendChannel<TileResult<R>>
    ) = launch {
        try {
            when (val tileResult = getTile(tileToProcess.zoom, tileToProcess.row, tileToProcess.col)) {
                is TileResult.Success -> {
                    val processed = processTile(tileResult.tile)
                    tilesProcessResult.send(TileResult.Success(processed))
                }
                is TileResult.Failure -> {
                    tilesProcessResult.send(tileResult)
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            println("Failed to process tile: $ex")
            tilesProcessResult.send(TileResult.Failure(tileToProcess))
        }
    }
}
