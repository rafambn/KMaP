package com.rafambn.kmap.components

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileResult
import com.rafambn.kmap.mapSource.tiled.vector.VectorTileResult
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.Degrees
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.style.Style

sealed interface Parameters

open class MarkerParameters(
    val coordinates: Coordinates,
    val alpha: Float = 1F,
    val drawPosition: DrawPosition = DrawPosition.TOP_LEFT,
    val zIndex: Float = 2F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val zoomToFix: Float? = null,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0,
    val clusterId: Int? = null
) : Parameters

open class ClusterParameters(
    val id: Int,
    val alpha: Float = 1F,
    val zIndex: Float = 2F,
    val rotateWithMap: Boolean = false,
    val rotation: Degrees = 0.0
) : Parameters

open class PathParameters(
    val path: Path,
    val color: Color,
    val zIndex: Float = 1F,
    val alpha: Float = 1F,
    val style: DrawStyle = Fill,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DefaultBlendMode,
    val clickPadding: Float = 10F,
    val zoomVisibilityRange: ClosedFloatingPointRange<Float> = 0F..Float.MAX_VALUE,
    val checkForClickInsidePath: Boolean = false,
) : Parameters{
    internal var drawPoint: ProjectedCoordinates = ProjectedCoordinates(0f, 0f)
    internal var totalPadding: Float = 0f
}

open class CanvasParameters(
    val id: Int,
    val alpha: Float,
    val zIndex: Float,
    val maxCacheTiles: Int,
) : Parameters

open class RasterCanvasParameters(
    id: Int,
    alpha: Float = 1F,
    zIndex: Float = 0F,
    maxCacheTiles: Int = 20,
    val tileSource: suspend (zoom: Int, row: Int, column: Int) -> RasterTileResult,
) : CanvasParameters(id, alpha, zIndex, maxCacheTiles)

open class VectorCanvasParameters(
    id: Int,
    alpha: Float = 1F,
    zIndex: Float = 0F,
    maxCacheTiles: Int = 20,
    val tileSource: suspend (zoom: Int, row: Int, column: Int) -> VectorTileResult,
    val style: Style,
) : CanvasParameters(id, alpha, zIndex, maxCacheTiles)
