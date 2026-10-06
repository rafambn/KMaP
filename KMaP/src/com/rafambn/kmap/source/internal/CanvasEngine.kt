package com.rafambn.kmap.source.internal

import androidx.compose.runtime.mutableStateMapOf
import com.rafambn.kmap.components.parameters.*
import com.rafambn.kmap.geometry.plane.TilePoint
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.source.*
import com.rafambn.kmap.source.preparation.PreparedTile
import com.rafambn.kmap.utils.loopInZoom
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.time.TimeSource

/**
 * One owner per map. Coordination and Compose conversion run in the supplied scope; encoded vector
 * preparation runs in owned platform workers. Register/read snapshots on the map's UI dispatcher.
 * Sources use normalized coordinates. Old viewport content may fill caches but cannot restore selection.
 */
class CanvasEngine internal constructor(
    coroutineScope: CoroutineScope,
    private val pool: PreparationPool = PreparationPool(),
) {
    private val job = SupervisorJob(coroutineScope.coroutineContext[Job])
    private val scope = CoroutineScope(coroutineScope.coroutineContext + job)
    private val canvases = mutableStateMapOf<Int, CanvasEntry>()
    private val configurations = Channel<CanvasConfiguration>(Channel.CONFLATED)
    private var submittedParameters = emptyList<CanvasParameters>()
    private var submittedGenerations = emptyMap<Int, Long>()
    private var generationSerial = 0L
    private val selections = Channel<TileSelection>(Channel.CONFLATED)
    // Bounded by admitted work: six fetches and two preparations can complete at once.
    private val results = Channel<CanvasResult>(8)
    private val fetches = mutableMapOf<Long, CanvasRequest>()
    private val processing = mutableMapOf<Long, CanvasRequest>()
    private val payloads = linkedMapOf<CanvasRequest, EncodedVectorTile>()
    private var selection: TileSelection? = null
    private var serial = 0L

    init {
        scope.launch {
            try {
                while (isActive) {
                    select<Unit> {
                        configurations.onReceive { configure(it) }
                        selections.onReceive { updateSelection(it) }
                        results.onReceive { accept(it) }
                    }
                    admitWork()
                    publishStatus()
                }
            } finally {
                pool.close()
                configurations.cancel()
                selections.cancel()
                results.cancel()
                job.cancel()
            }
        }
    }

    fun getActiveTiles(id: Int): ActiveTiles = canvases[id]?.activeTiles ?: ActiveTiles()
    fun getStatus(id: Int): TilePipelineStatus = canvases[id]?.status ?: TilePipelineStatus()

    /** Registration is asynchronous; reads during the same frame return a valid empty snapshot. */
    fun refreshCanvas(parameters: List<CanvasParameters>) {
        require(parameters.map { it.id }.distinct().size == parameters.size) { "Canvas must have different ids" }
        require(parameters.all { it.maxCacheTiles >= 0 }) { "maxCacheTiles must be nonnegative" }
        if (!job.isActive) return
        val previous = submittedParameters.associateBy { it.id }
        val generations = parameters.associate { current ->
            val old = previous[current.id]
            val unchangedSource = old != null &&
                (old is VectorCanvasParameters) == (current is VectorCanvasParameters)
            current.id to if (unchangedSource) submittedGenerations.getValue(current.id) else ++generationSerial
        }
        submittedParameters = parameters.toList()
        submittedGenerations = generations
        configurations.trySend(CanvasConfiguration(submittedParameters, generations))
    }

    fun invalidateTiles(canvasId: Int? = null) {
        if (!job.isActive) return
        submittedGenerations = submittedGenerations.mapValues { (id, generation) ->
            if (canvasId == null || id == canvasId) ++generationSerial else generation
        }
        configurations.trySend(CanvasConfiguration(submittedParameters, submittedGenerations))
    }

    internal fun resolveVisibleTiles(topLeft: TilePoint, bottomRight: TilePoint, zoomLevel: Int, mapProperties: MapProperties) {
        renderTiles(getVisibleTilesForLevel(topLeft, bottomRight, zoomLevel, mapProperties.tileRepeatMode, mapProperties.tileSize), zoomLevel)
    }

    /** Replaces pending viewport updates. Source/worker failures have at most two attempts per revision. */
    fun renderTiles(visibleTiles: List<TileSpecs>, zoomLevel: Int) {
        if (!job.isActive) return
        selections.trySend(TileSelection(visibleTiles.toList(), zoomLevel))
    }

    /** Closes this map's executors and cancels owned fetches without cancelling the caller's scope. */
    fun close() { job.cancel() }

    private fun configure(configuration: CanvasConfiguration) {
        val parameters = configuration.parameters
        val ids = parameters.map { it.id }.toSet()
        for (id in canvases.keys.toList()) if (id !in ids) removeCanvas(id)
        for (parameter in parameters) {
            var entry = canvases[parameter.id]
            if (entry != null && entry.generation != configuration.generations.getValue(parameter.id)) {
                removeCanvas(parameter.id)
                entry = null
            }
            if (entry == null) {
                entry = CanvasEntry(parameter, configuration.generations.getValue(parameter.id), ++serial)
                canvases[parameter.id] = entry
            } else {
                val previous = entry.parameters as? VectorCanvasParameters
                val current = parameter as? VectorCanvasParameters
                val changedStyle = previous != null && current != null &&
                    if (previous.style.preparation != null && current.style.preparation != null)
                        previous.style.preparation != current.style.preparation
                    else previous.style !== current.style
                if (changedStyle) {
                    entry.styleRevision = ++serial
                    entry.cache.clear()
                    entry.cacheSizes.clear()
                    entry.activeTiles = ActiveTiles(currentZoom = selection?.zoomLevel ?: 0)
                    entry.failures.clear()
                    entry.attempts.clear()
                    payloads.keys.removeAll { it.canvasId == parameter.id }
                }
                entry.parameters = parameter
                trimCache(entry)
            }
            updateActive(entry)
        }
    }

    private fun removeCanvas(id: Int) {
        canvases.remove(id)
        fetches.values.filter { it.canvasId == id }.forEach { it.job?.cancel() }
        payloads.keys.removeAll { it.canvasId == id }
        // CPU work finishes with its old generation; one canvas never terminates shared workers.
    }

    private fun updateSelection(latest: TileSelection) {
        selection = latest
        canvases.values.forEach { entry ->
            entry.failures.entries.removeAll { (_, failure) ->
                failure.stage == "source" && failure.attempts < 2
            }
            updateActive(entry)
            entry.failures.keys.retainAll(entry.demand.toSet())
            entry.attempts.keys.retainAll(entry.demand.toSet())
        }
        payloads.keys.removeAll { request -> currentEntry(request)?.demand?.contains(request.specs) != true }
    }

    private fun currentEntry(request: CanvasRequest): CanvasEntry? = canvases[request.canvasId]?.takeIf {
        it.generation == request.generation
    }

    private fun request(entry: CanvasEntry, specs: TileSpecs) = CanvasRequest(
        ++serial, entry.parameters.id, entry.generation, entry.styleRevision, specs,
    )

    private fun admitWork() {
        val candidates = canvases.values.flatMap { entry ->
            entry.demand.mapIndexed { rank, specs -> Triple(rank, entry, specs) }
        }.sortedBy { it.first }
        for ((_, entry, specs) in candidates) {
            if (entry.failures.containsKey(specs) || busy(entry, specs)) continue
            val raw = entry.raw[specs]
            if (raw != null) {
                if (payloads.size + fetches.size < 8) payloads[request(entry, specs)] = raw
            } else if (fetches.size < 6 && payloads.size + fetches.size < 8) {
                startFetch(entry, specs)
            }
        }
        while (processing.size < pool.capacity && payloads.isNotEmpty()) {
            val next = payloads.entries.first()
            val request = next.key
            val tile = next.value
            payloads.remove(request)
            val entry = currentEntry(request) ?: continue
            if (request.styleRevision != entry.styleRevision || request.specs !in entry.demand) continue
            val style = (entry.parameters as VectorCanvasParameters).style.preparation
            if (style == null) {
                fail(entry, request.specs, "preparation", "Encoded vector sources require a portable preparation style")
                continue
            }
            processing[request.id] = request
            request.job = scope.launch {
                val start = TimeSource.Monotonic.markNow()
                var prepared: PreparedTile? = null
                var error: Exception? = null
                try {
                    prepared = pool.prepare(PreparationRequest(request.id, request.generation,
                        request.specs, request.styleRevision, style, tile.bytes))
                } catch (cancelled: CancellationException) {
                    error = cancelled
                    throw cancelled
                } catch (failure: Exception) {
                    error = failure
                } finally {
                    results.trySend(CanvasResult.Prepared(request, prepared, error, start.elapsedNow().inWholeNanoseconds / 1e6))
                }
            }
        }
    }

    private fun busy(entry: CanvasEntry, specs: TileSpecs): Boolean =
        fetches.values.any { it.generation == entry.generation && it.specs == specs } ||
        (processing.values + payloads.keys).any {
            it.generation == entry.generation && it.specs == specs && it.styleRevision == entry.styleRevision
        }

    private fun startFetch(entry: CanvasEntry, specs: TileSpecs) {
        val request = request(entry, specs)
        val parameters = entry.parameters
        entry.attempts[specs] = (entry.attempts[specs] ?: 0) + 1
        fetches[request.id] = request
        request.job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val start = TimeSource.Monotonic.markNow()
            var result: TileResult<Tile>? = null
            var error: Exception? = null
            try {
                result = when (parameters) {
                    is RasterCanvasParameters -> parameters.tileSource(specs.zoom, specs.row, specs.col)
                    is VectorCanvasParameters -> parameters.tileSource(specs.zoom, specs.row, specs.col)
                }
            } catch (cancelled: CancellationException) {
                error = cancelled
                throw cancelled
            } catch (failure: Exception) {
                error = failure
            } finally {
                results.trySend(CanvasResult.Fetched(request, result, error, start.elapsedNow().inWholeNanoseconds / 1e6))
            }
        }
    }

    private suspend fun accept(result: CanvasResult) {
        val request = when (result) {
            is CanvasResult.Fetched -> result.request.also { fetches.remove(it.id) }
            is CanvasResult.Prepared -> result.request.also { processing.remove(it.id) }
        }
        val entry = currentEntry(request) ?: return
        when (result) {
            is CanvasResult.Fetched -> {
                entry.fetchMillis += result.millis
                val tile = (result.result as? TileResult.Success)?.tile
                if (tile == null) {
                    if (result.result != null || result.error != null)
                        fail(entry, request.specs, "source", result.error?.message ?: "Tile source returned failure")
                    return
                }
                if (TileSpecs(tile.zoom, tile.row, tile.col) != request.specs) {
                    fail(entry, request.specs, "source", "Source returned different tile coordinates")
                    return
                }
                when (tile) {
                    is EncodedVectorTile -> {
                        if (tile.bytes.size > 16 * 1024 * 1024) {
                            fail(entry, request.specs, "source", "Encoded tile exceeds the 16 MiB payload limit")
                            return
                        }
                        entry.raw.remove(request.specs)
                        entry.raw[request.specs] = tile
                        trimRaw(entry)
                        if (request.specs in entry.demand) payloads[request(entry, request.specs)] = tile
                    }
                    is VectorTile -> {
                        val start = TimeSource.Monotonic.markNow()
                        try {
                            cache(entry, optimizeMVTile(tile, (entry.parameters as VectorCanvasParameters).style), 0)
                            entry.completedLocal++
                        } catch (error: Exception) {
                            fail(entry, request.specs, "preparation", error.message ?: "Decoded tile compatibility preparation failed")
                        }
                        entry.conversionMillis += start.elapsedNow().inWholeNanoseconds / 1e6
                    }
                    else -> cache(entry, tile, 0)
                }
            }
            is CanvasResult.Prepared -> {
                if (request.styleRevision != entry.styleRevision) return
                entry.preparationMillis += result.millis
                if (result.tile == null) {
                    fail(entry, request.specs, if (result.error is WorkerFailure) "worker" else "preparation",
                        result.error?.message ?: "Preparation stopped")
                    if (result.error is WorkerFailure && (entry.attempts[request.specs] ?: 0) < 2 && request.specs in entry.demand) {
                        entry.attempts[request.specs] = 2
                        entry.failures.remove(request.specs)
                    }
                    return
                }
                val start = TimeSource.Monotonic.markNow()
                try {
                    cache(entry, toOptimizedTile(request.specs, result.tile), result.tile.features.sumOf {
                        it.coordinates.size * 4 + it.parts.size * 4 + it.properties.values.sumOf { value -> value.value.length * 2 + 16 }
                    })
                    entry.completedWorkers++
                } catch (error: Exception) {
                    fail(entry, request.specs, "conversion", error.message ?: "Compose conversion failed")
                }
                entry.conversionMillis += start.elapsedNow().inWholeNanoseconds / 1e6
                yield()
            }
        }
        updateActive(entry)
    }

    private fun fail(entry: CanvasEntry, specs: TileSpecs, stage: String, message: String) {
        if (specs !in entry.demand) return
        entry.failures[specs] = TilePipelineFailure(specs, stage, message, entry.attempts[specs] ?: 1)
    }

    private fun cache(entry: CanvasEntry, tile: Tile, size: Int) {
        val specs = TileSpecs(tile.zoom, tile.row, tile.col)
        entry.cache.remove(specs)
        entry.cache[specs] = tile
        entry.cacheSizes[specs] = size
        // Publish before eviction: visible and fallback graphics can outlive cache membership.
        updateActive(entry)
        trimCache(entry)
    }

    private fun trimCache(entry: CanvasEntry) {
        while (entry.cache.isNotEmpty() && (entry.cache.size > entry.parameters.maxCacheTiles || entry.cacheSizes.values.sum() > 32 * 1024 * 1024)) {
            val oldest = entry.cache.keys.first()
            entry.cache.remove(oldest)
            entry.cacheSizes.remove(oldest)
        }
        trimRaw(entry)
    }

    private fun trimRaw(entry: CanvasEntry) {
        while (entry.raw.isNotEmpty() && (entry.raw.size > entry.parameters.maxCacheTiles || entry.raw.values.sumOf { it.bytes.size } > 16 * 1024 * 1024))
            entry.raw.remove(entry.raw.keys.first())
    }

    private fun updateActive(entry: CanvasEntry) {
        val current = selection ?: return
        val active = entry.activeTiles.tiles.associateBy { TileSpecs(it.zoom, it.row, it.col) }
        val front = mutableListOf<Tile>()
        val missing = mutableListOf<TileSpecs>()
        for (specs in current.visibleTiles) {
            val tile = active[specs] ?: entry.cache[normalizedSpecs(specs)]?.withSpecs(specs)
            if (tile != null) front.add(tile) else missing.add(specs)
        }
        val available = (entry.activeTiles.tiles + entry.cache.values).distinctBy { TileSpecs(it.zoom, it.row, it.col) }.sortedByDescending { it.zoom }
        val parents = mutableMapOf<TileSpecs, Tile>()
        val children = mutableMapOf<TileSpecs, Tile>()
        for (specs in missing) for (tile in available) {
            if (tile.isParentOf(specs)) {
                parents[TileSpecs(tile.zoom, tile.row, tile.col)] = tile
                break
            } else if (tile.isChildOf(specs)) children[TileSpecs(tile.zoom, tile.row, tile.col)] = tile
        }
        children.values.removeAll { child -> parents.values.any { it.isParentOf(child) } }
        entry.activeTiles = ActiveTiles(currentZoom = current.zoomLevel, tiles = (front + parents.values + children.values).sortedBy { it.zoom })
        val centerRow = current.visibleTiles.map { it.row.toDouble() }.average()
        val centerCol = current.visibleTiles.map { it.col.toDouble() }.average()
        entry.demand = missing.sortedBy { (it.row - centerRow) * (it.row - centerRow) + (it.col - centerCol) * (it.col - centerCol) }
            .map(::normalizedSpecs).distinct()
    }

    private fun publishStatus() {
        for (entry in canvases.values) entry.status = TilePipelineStatus(
            fetching = fetches.values.count { it.generation == entry.generation },
            pendingPreparation = payloads.keys.count { it.generation == entry.generation },
            preparing = processing.values.count { it.generation == entry.generation },
            workerTiles = entry.completedWorkers, compatibilityTiles = entry.completedLocal,
            rawCacheBytes = entry.raw.values.sumOf { it.bytes.size }, preparedCacheBytes = entry.cacheSizes.values.sum(),
            fetchMillis = entry.fetchMillis, preparationMillis = entry.preparationMillis, conversionMillis = entry.conversionMillis,
            failures = entry.failures.values.toList(),
        )
    }
}

private fun normalizedSpecs(specs: TileSpecs) = TileSpecs(specs.zoom, specs.row.loopInZoom(specs.zoom), specs.col.loopInZoom(specs.zoom))
