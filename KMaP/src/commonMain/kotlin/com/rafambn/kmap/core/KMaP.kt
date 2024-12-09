package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import com.rafambn.kmap.tiles.TileCanvas
import com.rafambn.kmap.lazyMarker.KMaPScope
import com.rafambn.kmap.lazyMarker.rememberItemProviderLambda

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    motionController: MotionController,
    mapState: MapState,
    content: KMaPScope.() -> Unit
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        motionController.setMap(mapState)
        mapState.setDensity(density)
    }

    val itemProvider = rememberItemProviderLambda(content, mapState)

    itemProvider.invoke().canvasList.forEach {
        TileCanvas(
            getTile = it.getTile,
            cameraState = mapState.cameraState,
            mapProperties = mapState.mapProperties,
            positionOffset = mapState.drawReference,
            boundingBox = mapState.getBoundingBox(),
            canvasParameters = it.canvasParameters,
            gestureDetector = it.gestureDetection,
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    mapState.setCanvasSize(
                        Offset(
                            coordinates.size.width.toFloat(),
                            coordinates.size.height.toFloat()
                        )
                    )
                },
        )
    }
//    LazyLayout(
//        itemProvider = itemProvider,
//        modifier = modifier
//            .clipToBounds(),
//        prefetchState = null,
//        measurePolicy = fun LazyLayoutMeasureScope.(constraints: Constraints): MeasureResult {
//            val boundaries = mapState.getBoundingBox()
//            val indexes = itemProvider.getItemIndexesInRange(boundaries)
//
//            val indexesWithPlaceables = indexes.associateWith {
//                measure(it, Constraints())
//            }
//
//            return layout(constraints.maxWidth, constraints.maxHeight) {
//                indexesWithPlaceables.forEach { (index, placeables) ->
//                    val item = itemProvider.getItem(index)
//                    item?.let { placeItem(state, item, placeables) }
//                }
//            }
//        }
//    )

//    kmapContent.updateCluster(mapState)
//    Layout(
//        content = {
//            kmapContent.visibleMarkers.forEach {
//                Layout(
//                    content = { it.markerContent.invoke(it.markerParameters) },
//                    modifier = Modifier.Companion.componentInfo(
//                        MapComponentInfo(
//                            it.markerParameters, it.placementOffset.asOffset(), ComponentType.MARKER
//                        )
//                    ),
//                    measurePolicy = { measurables, constraints ->
//                        if (measurables.isEmpty())
//                            return@Layout layout(0, 0) {}
//                        val listPlaceables = measurables.map { measurable ->
//                            measurable.measure(constraints)
//                        }
//                        val maxWidth = listPlaceables.maxOf { placeable -> placeable.width }
//                        val maxHeight = listPlaceables.maxOf { placeable -> placeable.height }
//
//                        layout(maxWidth, maxHeight) {
//                            listPlaceables.forEach { placeable ->
//                                placeable.place(
//                                    x = 0,
//                                    y = 0
//                                )
//                            }
//                        }
//                    }
//                )
//            }
//            kmapContent.visibleClusters.forEach {
//                Layout(
//                    content = { it.clusterContent.invoke(it.clusterParameters, it.size) },
//                    modifier = Modifier.componentInfo(MapComponentInfo(it.clusterParameters, it.placementOffset.asOffset(), ComponentType.CLUSTER)),
//                    measurePolicy = { measurables, constraints ->
//                        if (measurables.isEmpty())
//                            return@Layout layout(0, 0) {}
//                        val listPlaceables = measurables.map { measurable ->
//                            measurable.measure(constraints)
//                        }
//                        val maxWidth = listPlaceables.maxOf { placeable -> placeable.width }
//                        val maxHeight = listPlaceables.maxOf { placeable -> placeable.height }
//
//                        layout(maxWidth, maxHeight) {
//                            listPlaceables.forEach { placeable ->
//                                placeable.place(
//                                    x = 0,
//                                    y = 0
//                                )
//                            }
//                        }
//                    }
//                )
//            }
//        },
//        modifier
//            .clipToBounds()
//            .wrapContentSize()
//            .onGloballyPositioned { coordinates ->
//                mapState.setCanvasSize(
//                    Offset(
//                        coordinates.size.width.toFloat(),
//                        coordinates.size.height.toFloat()
//                    )
//                )
//            }
//    ) { measurables, constraints ->
//        val markersComponent = measurables
//            .filter { it.componentInfo.type == ComponentType.MARKER }
//            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }
//
//        val clusterComponent = measurables
//            .filter { it.componentInfo.type == ComponentType.CLUSTER }
//            .map { Component(it.componentInfo.data, it.componentInfo.placementOffset, it.measure(constraints)) }
//
//        layout(constraints.maxWidth, constraints.maxHeight) {
//            markersComponent.forEach {
//                val componentData = it.data as MarkerParameters
//                val componentOffset = it.placementOffset
//                it.placeable.placeWithLayer(
//                    x = 0,
//                    y = 0,
//                    zIndex = componentData.zIndex
//                ) {
//                    alpha = componentData.alpha
//                    translationX = componentOffset.x - componentData.drawPosition.x * it.placeable.width
//                    translationY = componentOffset.y - componentData.drawPosition.y * it.placeable.height
//                    transformOrigin = componentData.drawPosition.asTransformOrigin()
//                    componentData.zoomToFix?.let { zoom ->
//                        scaleX = 2F.pow(mapState.cameraState.zoom - zoom)
//                        scaleY = 2F.pow(mapState.cameraState.zoom - zoom)
//                    }
//                    rotationZ =
//                        if (componentData.rotateWithMap)
//                            (mapState.cameraState.angleDegrees + componentData.rotation).toFloat()
//                        else
//                            componentData.rotation.toFloat()
//                }
//            }
//            clusterComponent.forEach {
//                val componentData = it.data as ClusterParameters
//                val componentOffset = it.placementOffset
//                it.placeable.placeWithLayer(
//                    x = 0,
//                    y = 0,
//                    zIndex = componentData.zIndex
//                ) {
//                    alpha = componentData.alpha
//                    translationX = componentOffset.x - componentData.drawPosition.x * it.placeable.width
//                    translationY = componentOffset.y - componentData.drawPosition.y * it.placeable.height
//                    transformOrigin = componentData.drawPosition.asTransformOrigin()
//                    rotationZ =
//                        if (componentData.rotateWithMap)
//                            (mapState.cameraState.angleDegrees + componentData.rotation).toFloat()
//                        else
//                            componentData.rotation.toFloat()
//                }
//            }
//        }
//    }
}