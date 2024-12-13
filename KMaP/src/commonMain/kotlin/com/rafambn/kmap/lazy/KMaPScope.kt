package com.rafambn.kmap.lazy

import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.tiles.TileRenderResult

@LazyScopeMarker
interface KMaPScope {

    fun canvas(
        canvasParameters: CanvasParameters = CanvasParameters(),
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    )

    fun marker(
        markerParameters: MarkerParameters,
        markerContent: @Composable (marker: MarkerParameters) -> Unit
    )

    fun markers(
        markerParameters: List<MarkerParameters>,
        markerContent: @Composable (marker: MarkerParameters) -> Unit
    )

    fun cluster(
        clusterParameters: ClusterParameters,
        clusterContent: @Composable (cluster: ClusterParameters) -> Unit
    )

    fun path(pathParameters: PathParameters)
}