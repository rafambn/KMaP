package com.rafambn.kmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.GraphiteDiagnosticTileSource
import com.rafambn.kmap.customSources.OSMMapProperties
import com.rafambn.kmap.customSources.SimpleZoomLevelRange
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.render.MapRenderBackend
import com.rafambn.kmap.utils.style.OptimizedStyle
import com.rafambn.kmap.utils.style.Style
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.style.StyleResolver
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun GraphiteDiagnosticScreen(navigateBack: () -> Unit) {
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            zoomLevels = SimpleZoomLevelRange(max = 6),
            tileSize = TileDimension(512.dp, 512.dp),
        )
    )
    val tileSource = remember { GraphiteDiagnosticTileSource() }
    val lowerStyle = remember { diagnosticLowerStyle() }
    val upperStyle = remember { diagnosticUpperStyle() }

    LaunchedEffect(mapState) {
        mapState.setZoom(2f)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = navigateBack) {
                Text("Back")
            }
            Button(onClick = { mapState.setZoom(mapState.cameraState.zoom - 1f) }) {
                Text("Zoom -")
            }
            Button(onClick = { mapState.setZoom(mapState.cameraState.zoom + 1f) }) {
                Text("Zoom +")
            }
            Button(onClick = { mapState.setAngle(mapState.cameraState.angleDegrees + 15.0) }) {
                Text("Rotate")
            }
        }
        KMaP(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            mapState = mapState,
            renderBackend = MapRenderBackend.Graphite,
        ) {
            vectorCanvas(
                parameters = VectorCanvasParameters(
                    id = 100,
                    zIndex = 0f,
                    tileSource = tileSource::getTile,
                    style = lowerStyle,
                ),
                gestureWrapper = getGestureDetector(mapState.motionController),
            )
            vectorCanvas(
                parameters = VectorCanvasParameters(
                    id = 101,
                    zIndex = 1f,
                    tileSource = tileSource::getTile,
                    style = upperStyle,
                ),
                gestureWrapper = getGestureDetector(mapState.motionController),
            )
        }
    }
}

private fun diagnosticLowerStyle(): OptimizedStyle = StyleResolver().resolve(
    Style(
        version = 8,
        name = "Graphite diagnostic lower canvas",
        sources = emptyMap(),
        layers = listOf(
            backgroundLayer(),
            fillLayer("lower-fill", "#4f7cac", 0.85),
            lineLayer("butt", "butt", "miter", "#f4d35e"),
            lineLayer("round-cap", "round", "miter", "#ee964b"),
            lineLayer("square", "square", "miter", "#f95738"),
        ),
    )
)

private fun diagnosticUpperStyle(): OptimizedStyle = StyleResolver().resolve(
    Style(
        version = 8,
        name = "Graphite diagnostic upper canvas",
        sources = emptyMap(),
        layers = listOf(
            fillLayer("upper-fill", "#8ac926", 0.42),
            lineLayer("miter", "butt", "miter", "#ffca3a"),
            lineLayer("round-join", "butt", "round", "#ff595e"),
            lineLayer("bevel", "butt", "bevel", "#6a4c93"),
        ),
    )
)

private fun backgroundLayer() = StyleLayer(
    id = "background",
    type = "background",
    paint = mapOf("background-color" to JsonPrimitive("#101820")),
)

private fun fillLayer(id: String, color: String, opacity: Double) = StyleLayer(
    id = id,
    type = "fill",
    sourceLayer = "fills",
    paint = mapOf(
        "fill-color" to JsonPrimitive(color),
        "fill-opacity" to JsonPrimitive(opacity),
        "fill-outline-color" to JsonPrimitive("#f8f9fa"),
    ),
)

private fun lineLayer(
    sourceLayer: String,
    cap: String,
    join: String,
    color: String,
) = StyleLayer(
    id = sourceLayer,
    type = "line",
    sourceLayer = sourceLayer,
    layout = mapOf(
        "line-cap" to JsonPrimitive(cap),
        "line-join" to JsonPrimitive(join),
    ),
    paint = mapOf(
        "line-color" to JsonPrimitive(color),
        "line-width" to JsonPrimitive(48.0),
    ),
)
