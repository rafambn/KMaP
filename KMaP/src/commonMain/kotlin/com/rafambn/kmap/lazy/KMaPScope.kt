package com.rafambn.kmap.lazy

import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Coordinates

@LazyScopeMarker
interface KMaPScope {

    fun canvas(
        alpha: Float = 1F,
        zIndex: Float = 0F,
        maxCacheTiles: Int = 20,
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

    fun path(
        origin: Coordinates,
        path: Path,
        color: Color,
        zIndex: Float = 1F,
        alpha: Float = 1F,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        gestureDetection: (suspend PointerInputScope.() -> Unit)? = null
    )
}