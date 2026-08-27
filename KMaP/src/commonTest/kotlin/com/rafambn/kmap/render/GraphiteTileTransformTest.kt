package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.utils.vectorTile.OptimizedMVTile
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphiteTileTransformTest {
    @Test
    fun currentParentAndChildTilesUseComposeMath() {
        val camera = camera(positionOffset = Offset(-10.2f, 7.8f))

        val current = resolveGraphiteTileTransform(camera, 3, tile(3, row = 2, col = 1))!!
        assertEquals(1f, current.scaleAdjustment)
        assertEquals(245f, current.left)
        assertEquals(519f, current.top)
        assertEquals(0.0625f, current.scaleX)

        val parent = resolveGraphiteTileTransform(camera, 3, tile(2, row = 1, col = 0))!!
        assertEquals(2f, parent.scaleAdjustment)
        assertEquals(-11f, parent.left)
        assertEquals(519f, parent.top)
        assertEquals(0.125f, parent.scaleX)

        val child = resolveGraphiteTileTransform(camera, 3, tile(4, row = -1, col = -2))!!
        assertEquals(0.5f, child.scaleAdjustment)
        assertEquals(-267f, child.left)
        assertEquals(-121f, child.top)
        assertEquals(0.03125f, child.scaleX)
    }
}

private fun camera(positionOffset: Offset) = GraphiteMapCamera(
    canvasSize = IntSize(800, 600),
    translation = Offset(400f, 300f),
    rotationDegrees = 12f,
    magnifierScale = 0.5f,
    positionOffset = positionOffset,
    tileSizePx = Size(256f, 256f),
    zoom = 3.5,
)

private fun tile(zoom: Int, row: Int, col: Int) = OptimizedVectorTile(
    zoom = zoom,
    row = row,
    col = col,
    optimizedTile = OptimizedMVTile(extent = 4096),
)
