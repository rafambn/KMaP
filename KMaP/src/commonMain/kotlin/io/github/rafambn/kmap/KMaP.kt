package io.github.rafambn.kmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import io.github.rafambn.kmap.config.DefaultMapProperties
import io.github.rafambn.kmap.config.MapProperties
import io.github.rafambn.kmap.config.characteristics.MapSource
import io.github.rafambn.kmap.core.ComponentType
import io.github.rafambn.kmap.core.DrawPosition
import io.github.rafambn.kmap.core.MapComponentData
import io.github.rafambn.kmap.core.TileCanvas
import io.github.rafambn.kmap.core.componentData
import io.github.rafambn.kmap.core.motion.MotionController
import io.github.rafambn.kmap.core.state.MapState

@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapProperties: MapProperties = DefaultMapProperties(),
    mapState: MapState,
    mapSource: MapSource, //TODO(2): make have multiple tile canvas
    canvasGestureListener: DefaultCanvasGestureListener = DefaultCanvasGestureListener(),
    onCanvasChangeSize: (Offset) -> Unit,
    content: @Composable KMaPScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        canvasGestureListener.setMotionController(motionController)
        mapState.setProperties(mapProperties)
        mapState.setMapSource(mapSource)
        mapState.setDensity(density)
    }
    Layout(
        content = {
            TileCanvas( //TODO(2): make have multiple tile canvas
                Modifier
                    .componentData(MapComponentData(Offset.Zero, 0F, DrawPosition.TOP_LEFT, 0.0, ComponentType.CANVAS))
                ,
                mapState.tileCanvasStateFlow.collectAsState().value,
                canvasGestureListener
            )
            KMaPScope.content()
        },
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .wrapContentSize()
            .onGloballyPositioned { coordinates ->
                onCanvasChangeSize(coordinates.size.toSize().toRect().bottomRight)
            }
    ) { measurables, constraints ->
        val canvasData: MapComponentData
        val canvasPlaceable = measurables
            .first { it.componentData.componentType == ComponentType.CANVAS }
            .also { canvasData = it.componentData }
            .measure(constraints)

        val placersData: List<MapComponentData>
        val placersPlaceable = measurables
            .filter { it.componentData.componentType == ComponentType.PLACER }
            .also { measurableMarkers -> placersData = measurableMarkers.map { it.componentData } }
            .map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            canvasPlaceable.place(
                x = 0,
                y = 0,
                zIndex = canvasData.zIndex
            )
            placersPlaceable.forEachIndexed { index, placeable ->
                placeable.place(
                    x = placersData[index].position.x.toInt(),
                    y = placersData[index].position.y.toInt(),
                    zIndex = placersData[index].zIndex
                )
            }
        }
    }
}