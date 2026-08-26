@file:OptIn(com.rafambn.graphitesurface.ExperimentalGraphiteSurfaceApi::class)

package com.rafambn.kmap.render

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rafambn.graphitesurface.GraphiteEngineState
import com.rafambn.graphitesurface.GraphiteSurface
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.core.MapState
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

    LaunchedEffect(controller) {
        controller.engineState.collect { state ->
            if (state is GraphiteEngineState.Failed) controller.reportFatal(state.error)
        }
    }

    val scene = compileGraphiteMapScene(content)
    SideEffect { controller.update(scene) }

    val gestureWrapper = content.canvas.withIndex().maxWithOrNull(
        compareBy<IndexedValue<com.rafambn.kmap.components.Canvas>>(
            { it.value.parameters.zIndex },
            { it.index },
        )
    )?.value?.gestureWrapper

    GraphiteSurface(
        renderer = controller.renderer,
        modifier = modifier.mapGestures(gestureWrapper),
    )
}
