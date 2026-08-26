package com.rafambn.kmap.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.*
import com.rafambn.kmap.core.MapState
import com.rafambn.kmap.mapProperties.*
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.mapSource.tiled.TileResult
import com.rafambn.kmap.mapSource.tiled.tiles.TileSpecs
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class GraphiteCompatibilityTest {
    @Test
    fun requiresACanvas() = withContent({}) { content ->
        assertEquals("Graphite requires at least one canvas", content.graphiteIncompatibility())
    }

    @Test
    fun rasterWinsBeforeEveryOtherReason() = withContent({
        rasterCanvas(rasterParameters())
        marker(MarkerParameters(Coordinates.Zero)) {}
        path(PathParameters(Path(), Color.Red))
    }) { content ->
        assertEquals(
            "Graphite does not render maps containing rasterCanvas",
            content.graphiteIncompatibility(),
        )
    }

    @Test
    fun rejectsMarkersClustersAndPaths() {
        withContent({
            vectorCanvas(vectorParameters())
            marker(MarkerParameters(Coordinates.Zero)) {}
        }) { content ->
            assertEquals("Graphite does not render markers yet", content.graphiteIncompatibility())
        }

        withContent({
            vectorCanvas(vectorParameters())
            cluster(ClusterParameters(1)) {}
        }) { content ->
            assertEquals("Graphite does not render clusters yet", content.graphiteIncompatibility())
        }

        withContent({
            vectorCanvas(vectorParameters())
            path(PathParameters(Path(), Color.Red))
        }) { content ->
            assertEquals("Graphite does not render paths yet", content.graphiteIncompatibility())
        }
    }

    @Test
    fun rejectsPartialCanvasAlpha() = withContent({
        vectorCanvas(vectorParameters(alpha = 0.5f))
    }) { content ->
        assertEquals(
            "Graphite requires vector canvas alpha == 1: 1",
            content.graphiteIncompatibility(),
        )
    }

    @Test
    fun rejectsSymbolUnknownLayerAndUnknownProperty() {
        withContent({ vectorCanvas(vectorParameters(style = style(layer("labels", "symbol")))) }) {
            assertEquals(
                "Graphite does not render vector symbol layers yet: labels",
                it.graphiteIncompatibility(),
            )
        }
        withContent({ vectorCanvas(vectorParameters(style = style(layer("heat", "heatmap")))) }) {
            assertEquals(
                "Graphite does not render vector layer type 'heatmap' yet: heat",
                it.graphiteIncompatibility(),
            )
        }
        withContent({
            vectorCanvas(
                vectorParameters(
                    style = style(
                        layer(
                            id = "roads",
                            type = "line",
                            paint = mapOf("line-dasharray" to value(listOf(1.0, 2.0))),
                        )
                    )
                )
            )
        }) {
            assertEquals(
                "Graphite does not render property 'line-dasharray' yet: roads",
                it.graphiteIncompatibility(),
            )
        }
    }

    @Test
    fun acceptsBackgroundFillAndLine() = withContent({
        vectorCanvas(
            vectorParameters(
                style = style(
                    layer(
                        "background",
                        "background",
                        paint = mapOf("background-color" to value(Color.White)),
                    ),
                    layer(
                        "land",
                        "fill",
                        paint = mapOf("fill-color" to value(Color.Green)),
                    ),
                    layer(
                        "roads",
                        "line",
                        layout = mapOf("line-cap" to value("round")),
                        paint = mapOf("line-width" to value(2.0)),
                    ),
                )
            )
        )
    }) { content ->
        assertNull(content.graphiteIncompatibility())
    }
}

private fun withContent(
    declaration: KMaPContent.() -> Unit,
    assertion: (KMaPContent) -> Unit,
) {
    val scope = CoroutineScope(SupervisorJob())
    try {
        assertion(KMaPContent(declaration, testMapState(scope)))
    } finally {
        scope.cancel()
    }
}

private fun testMapState(scope: CoroutineScope) = MapState(
    mapProperties = object : MapProperties {
        override val boundMap = BoundMapBorder(MapBorderType.BOUND, MapBorderType.BOUND)
        override val outsideTiles = OutsideTilesType.NONE
        override val zoomLevels = object : ZoomLevelRange {
            override val min = 0
            override val max = 4
        }
        override val coordinatesRange = object : CoordinatesRange {
            override val latitude = Latitude(north = 90.0, south = -90.0)
            override val longitude = Longitude(west = -180.0, east = 180.0)
        }
        override val tileSize = TileDimension(256.dp, 256.dp)

        override fun toProjectedCoordinates(coordinates: Coordinates) =
            ProjectedCoordinates(coordinates.x, coordinates.y)

        override fun toCoordinates(projectedCoordinates: ProjectedCoordinates) =
            Coordinates(projectedCoordinates.x, projectedCoordinates.y)
    },
    coroutineScope = scope,
)

private fun rasterParameters() = RasterCanvasParameters(id = 1) { zoom, row, column ->
    TileResult.Failure(TileSpecs(zoom, row, column))
}

private fun vectorParameters(
    alpha: Float = 1f,
    style: OptimizedStyle = style(layer("background", "background")),
) = VectorCanvasParameters(
    id = 1,
    alpha = alpha,
    tileSource = { zoom, row, column ->
        TileResult.Failure(TileSpecs(zoom, row, column))
    },
    style = style,
)

private fun style(vararg layers: OptimizedStyleLayer) = OptimizedStyle(
    version = 8,
    name = "test",
    layers = layers.toList(),
    sources = emptyMap(),
)

private fun layer(
    id: String,
    type: String,
    layout: Map<String, CompiledValue<*>> = emptyMap(),
    paint: Map<String, CompiledValue<*>> = emptyMap(),
) = OptimizedStyleLayer(
    id = id,
    type = type,
    source = null,
    sourceLayer = null,
    minZoom = 0.0,
    maxZoom = 24.0,
    filter = null,
    layout = CompiledLayout(
        visibility = CompiledValue(evaluate = { _, _, _ -> true }),
        properties = layout,
    ),
    paint = CompiledPaint(paint),
)

private fun <T> value(value: T) = CompiledValue<T>(evaluate = { _, _, _ -> value })
