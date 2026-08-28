package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.utils.style.CompiledLayout
import com.rafambn.kmap.utils.style.CompiledPaint
import com.rafambn.kmap.utils.style.CompiledValue
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.vectorTile.OptimizedMVTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GraphiteVectorRenderItemTest {
    @Test
    fun cameraMovementReusesTileLayerRecordings() {
        val tile = tile()
        val scene = scene(tile, camera())
        val moved = scene.copy(
            camera = camera().copy(
                canvasSize = IntSize(1200, 900),
                translation = Offset(600f, 450f),
                rotationDegrees = 35f,
                magnifierScale = 0.75f,
                positionOffset = Offset(-900.4f, 420.8f),
            )
        )

        assertEquals(
            scene.vectorRenderItems().single().recordingKey,
            moved.vectorRenderItems().single().recordingKey,
        )
    }

    @Test
    fun styleZoomAndTileContentInvalidateRecordings() {
        val tile = tile()
        val original = scene(tile, camera()).vectorRenderItems().single().recordingKey
        val zoomed = scene(tile, camera().copy(zoom = 4.5))
            .vectorRenderItems().single().recordingKey
        val replaced = scene(tile(), camera()).vectorRenderItems().single().recordingKey

        assertNotEquals(original, zoomed)
        assertNotEquals(original, replaced)
    }
}

private fun scene(tile: OptimizedVectorTile, camera: GraphiteMapCamera): GraphiteMapScene {
    val layer = OptimizedStyleLayer(
        id = "road",
        type = "line",
        source = null,
        sourceLayer = null,
        minZoom = 0.0,
        maxZoom = 24.0,
        filter = null,
        layout = CompiledLayout(CompiledValue({ _, _, _ -> true }), emptyMap()),
        paint = CompiledPaint(emptyMap()),
    )
    return GraphiteMapScene(
        camera = camera,
        canvases = listOf(
            GraphiteVectorCanvas(
                id = 7,
                declarationIndex = 0,
                zIndex = 0f,
                style = OptimizedStyle(8, "test", listOf(layer), emptyMap()),
                activeTiles = ActiveTiles(currentZoom = 4, tiles = listOf(tile)),
            )
        ),
    )
}

private fun camera() = GraphiteMapCamera(
    canvasSize = IntSize(800, 600),
    translation = Offset(400f, 300f),
    rotationDegrees = 0f,
    magnifierScale = 0f,
    positionOffset = Offset.Zero,
    tileSizePx = Size(256f, 256f),
    zoom = 4.0,
)

private fun tile() = OptimizedVectorTile(
    zoom = 4,
    row = 3,
    col = 2,
    optimizedTile = OptimizedMVTile(extent = 4096),
)
