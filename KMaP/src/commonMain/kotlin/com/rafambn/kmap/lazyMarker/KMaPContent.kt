package com.rafambn.kmap.lazyMarker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.input.pointer.PointerInputScope
import com.rafambn.kmap.components.CanvasComponent
import com.rafambn.kmap.components.ClusterComponent
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.components.MarkerComponent
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.PathComponent
import com.rafambn.kmap.tiles.TileRenderResult
import com.rafambn.kmap.utils.Coordinates

class KMaPContent(
    content: KMaPScope.() -> Unit,
) : KMaPScope {

    val markers = mutableListOf<MarkerComponent>()
    val cluster = mutableListOf<ClusterComponent>()
    val canvas = mutableListOf<CanvasComponent>()
    val paths = mutableListOf<PathComponent>()

    init {
        apply(content)
    }

    override fun canvas(
        alpha: Float,
        zIndex: Float,
        maxCacheTiles: Int,
        tileSource: suspend (zoom: Int, row: Int, column: Int) -> TileRenderResult,
        gestureDetection: (suspend PointerInputScope.() -> Unit)?
    ) {
        canvas.add(CanvasComponent(alpha, zIndex, maxCacheTiles, tileSource, gestureDetection))
    }

    override fun marker(markerParameters: MarkerParameters, markerContent: @Composable () -> Unit) {
        markers.add(MarkerComponent(markerParameters, { markerContent() }))
    }

    override fun markers(markerParameters: List<MarkerParameters>, markerContent: @Composable (index: Int) -> Unit) {
        markerParameters.forEachIndexed { index, it ->
            markers.add(MarkerComponent(it, { markerContent(index) }))
        }
    }

    override fun cluster(clusterParameters: ClusterParameters, clusterContent: @Composable () -> Unit) {
        cluster.add(ClusterComponent(clusterParameters, { clusterContent() }))
    }

    override fun path(
        origin: Coordinates,
        path: Path,
        color: Color,
        zIndex: Float,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
//        gestureDetection: (suspend PointerInputScope.() -> Unit)?
    ) {
        paths.add(
            PathComponent(
                origin,
                path,
                color,
                zIndex,
                alpha,
                style,
                colorFilter,
                blendMode,
//                gestureDetection
            )
        )
    }
}