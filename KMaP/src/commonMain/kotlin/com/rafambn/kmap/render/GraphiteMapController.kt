@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.rafambn.kmap.render

import androidx.compose.ui.unit.IntSize
import com.rafambn.graphitesurface.GraphiteEngine
import com.rafambn.graphitesurface.GraphiteEngineState
import com.rafambn.graphitesurface.GraphiteRenderMode
import com.rafambn.graphitesurface.GraphiteRenderer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class GraphiteMapController(
    private val onFatalError: (Throwable) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val fatalErrorReported = AtomicBoolean(false)
    private val scene = AtomicReference<GraphiteMapScene?>(null)
    private val engine = GraphiteEngine(recorderCount = 2)
    private val vectorRenderer = GraphiteVectorRenderer(engine)
    private val mutableSurfaceSize = MutableStateFlow<IntSize?>(null)

    val engineState: StateFlow<GraphiteEngineState> = engine.diagnostics.state
    val surfaceSize: StateFlow<IntSize?> = mutableSurfaceSize.asStateFlow()

    val renderer = GraphiteRenderer(
        runtime = engine,
        renderMode = GraphiteRenderMode.OnDemand,
    ) { _, presentation ->
        mutableSurfaceSize.value = presentation.pixelSize
        val currentScene = scene.load() ?: return@GraphiteRenderer
        try {
            vectorRenderer.render(currentScene, presentation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            reportFatal(error)
        }
    }

    fun update(scene: GraphiteMapScene) {
        if (closed.load()) return
        if (this.scene.exchange(scene) != scene) renderer.requestRender()
    }

    fun reportFatal(error: Throwable) {
        if (!fatalErrorReported.compareAndSet(false, true)) return
        close()
        onFatalError(error)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scene.store(null)
        engine.close()
    }
}
