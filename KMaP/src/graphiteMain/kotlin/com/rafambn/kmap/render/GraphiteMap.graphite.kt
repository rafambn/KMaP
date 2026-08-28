@file:OptIn(com.rafambn.graphitesurface.ExperimentalGraphiteSurfaceApi::class)

package com.rafambn.kmap.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import com.rafambn.graphitesurface.GraphiteEngineState
import com.rafambn.graphitesurface.GraphiteSurface
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.mapSource.tiled.canvas.VectorTileCanvas
import com.rafambn.kmap.mapSource.tiled.canvas.mapGestures

@Composable
internal actual fun GraphiteMap(
    modifier: Modifier,
    mapState: MapState,
    content: KMaPContent,
    onFatalError: (Throwable) -> Unit,
) {
    val latestFatalError = rememberUpdatedState(onFatalError)
    val controllerResult = remember {
        runCatching {
            GraphiteMapController { error -> latestFatalError.value(error) }
        }
    }
    val controller = controllerResult.getOrElse { error ->
        SideEffect { latestFatalError.value(error) }
        return
    }

    DisposableEffect(controller) {
        onDispose(controller::close)
    }

    val engineState = controller.engineState.collectAsState().value
    if (engineState is GraphiteEngineState.Failed) {
        SideEffect { controller.reportFatal(engineState.error) }
        return
    }

    val canvases = content.canvas.withIndex().sortedWith(
        compareBy(
            { it.value.parameters.zIndex },
            { it.index },
        )
    )
    val gestureWrapper = canvases.lastOrNull()?.value?.gestureWrapper
    val scene = compileGraphiteMapScene(content)
    SideEffect {
        controller.update(scene)
    }

    Box(
        modifier = modifier.onSizeChanged { size ->
            mapState.setCanvasSize(Offset(size.width.toFloat(), size.height.toFloat()))
        },
    ) {
        GraphiteSurface(
            renderer = controller.renderer,
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
