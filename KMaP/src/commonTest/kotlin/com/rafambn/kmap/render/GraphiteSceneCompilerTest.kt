package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.mapSource.tiled.ActiveTiles
import com.rafambn.kmap.utils.style.CompiledLayout
import com.rafambn.kmap.utils.style.CompiledPaint
import com.rafambn.kmap.utils.style.CompiledValue
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphiteSceneCompilerTest {
    @Test
    fun batchesPreserveCanvasAndLayerOrder() {
        val lower = canvas(
            id = 2,
            declarationIndex = 1,
            zIndex = 0f,
            layers = listOf(layer("land", "fill"), layer("background", "background"), layer("road", "line")),
        )
        val tiedEarlier = canvas(
            id = 1,
            declarationIndex = 0,
            zIndex = 0f,
            layers = listOf(layer("water", "fill")),
        )
        val top = canvas(
            id = 3,
            declarationIndex = 2,
            zIndex = 4f,
            layers = listOf(layer("top", "line")),
        )
        val scene = GraphiteMapScene(camera(), listOf(tiedEarlier, lower, top))

        assertEquals(
            listOf("water", "background", "land", "road", "top"),
            scene.vectorBatches().map { it.styleLayer.id },
        )
        assertEquals(listOf(0, 1, 1, 1, 2), scene.vectorBatches().map { it.canvasOrder })
    }
}

private fun camera() = GraphiteMapCamera(
    canvasSize = IntSize(800, 600),
    translation = Offset(400f, 300f),
    rotationDegrees = 0f,
    magnifierScale = 0f,
    positionOffset = Offset.Zero,
    tileSizePx = Size(256f, 256f),
    zoom = 2.0,
)

private fun canvas(
    id: Int,
    declarationIndex: Int,
    zIndex: Float,
    layers: List<OptimizedStyleLayer>,
) = GraphiteVectorCanvas(
    id = id,
    declarationIndex = declarationIndex,
    zIndex = zIndex,
    style = OptimizedStyle(8, "test", layers, emptyMap()),
    activeTiles = ActiveTiles(),
)

private fun layer(id: String, type: String) = OptimizedStyleLayer(
    id = id,
    type = type,
    source = null,
    sourceLayer = null,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = CompiledLayout(CompiledValue({ _, _, _ -> true }), emptyMap()),
    paint = CompiledPaint(emptyMap()),
)
