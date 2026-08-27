@file:OptIn(com.rafambn.graphitesurface.ExperimentalGraphiteSurfaceApi::class)

package com.rafambn.kmap.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import com.rafambn.graphitesurface.GraphitePresentationState
import com.rafambn.graphitesurface.GraphiteRuntimeState
import com.rafambn.graphitesurface.GraphiteSurface
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.mapSource.tiled.canvas.VectorTileCanvas
import com.rafambn.kmap.mapSource.tiled.canvas.mapGestures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterIsInstance

@Composable
internal actual fun GraphiteMap(
    modifier: Modifier,
    mapState: MapState,
    content: KMaPContent,
    onFatalError: (Throwable) -> Unit,
) {
    val latestFatalError = rememberUpdatedState(onFatalError)
    var controller by remember { mutableStateOf<GraphiteMapController?>(null) }
    LaunchedEffect(Unit) {
        try {
            controller = GraphiteMapController.create { error -> latestFatalError.value(error) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            latestFatalError.value(error)
        }
    }

    val currentController = controller ?: return
    DisposableEffect(currentController) {
        onDispose(currentController::close)
    }
    when (val runtimeState = currentController.runtimeState.collectAsState().value) {
        is GraphiteRuntimeState.DeviceLost -> {
            SideEffect { currentController.reportFatal(runtimeState.error) }
            return
        }

        is GraphiteRuntimeState.Failed -> {
            SideEffect {
                currentController.reportFatal(
                    runtimeState.failure.cause
                        ?: IllegalStateException(runtimeState.failure.message),
                )
            }
            return
        }

        GraphiteRuntimeState.Ready,
        GraphiteRuntimeState.Closing,
        GraphiteRuntimeState.Closed -> Unit
    }

    val scene = compileGraphiteMapScene(content)
    LaunchedEffect(currentController, scene) {
        try {
            currentController.runtime.presentation
                .filterIsInstance<GraphitePresentationState.Attached>()
                .collect { presentation ->
                    currentController.render(scene, presentation.info)
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            currentController.reportFatal(error)
        }
    }
    if (currentController.isClosed) {
        return
    }

    val canvases = content.canvas.withIndex().sortedWith(
        compareBy(
            { it.value.parameters.zIndex },
            { it.index },
        )
    )
    val gestureWrapper = canvases.lastOrNull()?.value?.gestureWrapper

    Box(
        modifier = modifier.onSizeChanged { size ->
            mapState.setCanvasSize(Offset(size.width.toFloat(), size.height.toFloat()))
        },
    ) {
        GraphiteSurface(
            runtime = currentController.runtime,
            modifier = Modifier.fillMaxSize().mapGestures(gestureWrapper),
        )

        canvases.forEach { (_, canvas) ->
            val parameters = canvas.parameters as VectorCanvasParameters
            val symbolStyle = remember(parameters.style) {
                parameters.style.copy(
                    layers = parameters.style.layers.filter { it.type == "symbol" },
                )
            }
            if (symbolStyle.layers.isEmpty()) return@forEach

            key(parameters.id) {
                VectorTileCanvas(
                    canvasSize = mapState.cameraState.canvasSize,
                    gestureWrapper = null,
                    activeTiles = { mapState.canvasKernel.getActiveTiles(parameters.id) },
                    magnifierScale = mapState.drawMagScale,
                    positionOffset = mapState.drawReference,
                    tileSize = mapState.drawTileSize,
                    rotationDegrees = mapState.drawRotationDegrees,
                    translation = mapState.drawTranslation,
                    style = { symbolStyle },
                    zoom = { mapState.cameraState.zoom.toDouble() },
                )
            }
        }
    }
}
