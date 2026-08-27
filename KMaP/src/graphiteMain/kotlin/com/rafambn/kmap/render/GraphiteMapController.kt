@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.rafambn.kmap.render

import com.rafambn.graphitesurface.GraphitePresentationInfo
import com.rafambn.graphitesurface.GraphiteRuntime
import com.rafambn.graphitesurface.GraphiteRuntimeConfig
import com.rafambn.graphitesurface.GraphiteRuntimeState
import kotlin.concurrent.atomics.AtomicBoolean
import kotlinx.coroutines.flow.StateFlow

internal class GraphiteMapController private constructor(
    val runtime: GraphiteRuntime,
    private val onFatalError: (Throwable) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val fatalErrorReported = AtomicBoolean(false)
    private val vectorRenderer = GraphiteVectorRenderer(runtime)

    val isClosed: Boolean get() = closed.load()
    val runtimeState: StateFlow<GraphiteRuntimeState> = runtime.state

    suspend fun render(scene: GraphiteMapScene, presentation: GraphitePresentationInfo) {
        if (!closed.load()) vectorRenderer.render(scene, presentation)
    }

    fun reportFatal(error: Throwable) {
        if (!fatalErrorReported.compareAndSet(false, true)) return
        close()
        onFatalError(error)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runtime.close()
    }

    companion object {
        suspend fun create(onFatalError: (Throwable) -> Unit): GraphiteMapController = GraphiteMapController(
            runtime = GraphiteRuntime.create(GraphiteRuntimeConfig(recorderCount = 2)),
            onFatalError = onFatalError,
        )
    }
}
