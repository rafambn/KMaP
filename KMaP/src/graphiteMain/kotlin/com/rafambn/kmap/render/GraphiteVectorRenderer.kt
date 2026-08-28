package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.rafambn.graphitesurface.GraphiteDrawStyle
import com.rafambn.graphitesurface.GraphiteEncoder
import com.rafambn.graphitesurface.GraphiteEngine
import com.rafambn.graphitesurface.GraphitePresentationInfo
import com.rafambn.graphitesurface.GraphitePresentResult
import com.rafambn.graphitesurface.GraphiteRecording
import com.rafambn.graphitesurface.GraphiteTransform
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import kotlin.math.pow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class GraphiteVectorRenderer(
    private val engine: GraphiteEngine,
) {
    private val recordings = LinkedHashMap<GraphiteVectorRecordingKey, GraphiteRecording>()

    suspend fun render(
        scene: GraphiteMapScene,
        presentation: GraphitePresentationInfo,
        isCurrent: () -> Boolean,
    ) {
        val items = scene.vectorRenderItems()
        recordMissing(items, isCurrent)
        retainActiveAndRecent(items)
        if (!isCurrent()) return

        val transform = scene.camera.frameTransform()
        when (
            engine.present(presentation) {
                items.forEach { item ->
                    recordings[item.recordingKey]?.let { recording ->
                        insert(recording, transform = transform)
                    }
                }
            }
        ) {
            GraphitePresentResult.Accepted,
            GraphitePresentResult.ReplacedPending,
            GraphitePresentResult.NoPresentation,
            GraphitePresentResult.StalePresentation -> Unit

            GraphitePresentResult.RuntimeUnavailable ->
                error("Graphite engine became unavailable while presenting a map frame")
        }
    }

    private suspend fun recordMissing(
        items: List<GraphiteVectorRenderItem>,
        isCurrent: () -> Boolean,
    ) = coroutineScope {
        val missing = items.filter { it.recordingKey !in recordings }
        missing.groupBy(::recorderIndex)
            .map { (recorderIndex, recorderItems) ->
                async {
                    recorderItems.mapNotNull { item ->
                        if (!isCurrent()) return@mapNotNull null
                        item.recordingKey to engine.recorders[recorderIndex].record {
                            drawItem(item)
                        }
                    }
                }
            }
            .awaitAll()
            .flatten()
            .forEach { (key, recording) -> recordings[key] = recording }
    }

    private fun recorderIndex(item: GraphiteVectorRenderItem): Int {
        val tile = item.tile
        var hash = item.batch.canvasId
        hash = 31 * hash + item.batch.styleLayerIndex
        hash = 31 * hash + tile.zoom
        hash = 31 * hash + tile.row
        hash = 31 * hash + tile.col
        return hash.mod(engine.recorders.size)
    }

    private fun retainActiveAndRecent(items: List<GraphiteVectorRenderItem>) {
        val activeKeys = items.mapTo(mutableSetOf()) { it.recordingKey }
        activeKeys.forEach { key ->
            val recording = recordings.remove(key) ?: return@forEach
            recordings[key] = recording
        }
        val maximumSize = activeKeys.size + RECENT_RECORDING_CAPACITY
        while (recordings.size > maximumSize) {
            recordings.remove(recordings.keys.first())
        }
    }

    private companion object {
        const val RECENT_RECORDING_CAPACITY = 512
    }
}

internal fun GraphiteMapCamera.frameTransform(): GraphiteTransform {
    val magnifierScale = 2f.pow(magnifierScale)
    return GraphiteTransform.translation(translation.x, translation.y) *
        GraphiteTransform.rotationDegrees(rotationDegrees) *
        GraphiteTransform.scale(magnifierScale) *
        GraphiteTransform.translation(
            positionOffset.x.toIntFloor().toFloat(),
            positionOffset.y.toIntFloor().toFloat(),
        )
}

private fun GraphiteEncoder.drawItem(item: GraphiteVectorRenderItem) {
    if (item.batch.styleLayer.type == "background") {
        drawBackground(item)
    } else {
        drawFeatures(item)
    }
}

private fun GraphiteEncoder.drawBackground(item: GraphiteVectorRenderItem) {
    val key = item.recordingKey
    val layer = item.batch.styleLayer
    val color = layer.paint.properties["background-color"]
        ?.evaluate(key.styleZoom, emptyMap(), "") as? Color ?: Color.Magenta
    val opacity = layer.paint.properties["background-opacity"]
        ?.evaluate(key.styleZoom, emptyMap(), "") as? Number ?: 1f
    val scaleAdjustment = 2f.pow(key.currentZoom - item.tile.zoom)
    val width = key.tileWidth * scaleAdjustment
    val height = key.tileHeight * scaleAdjustment
    val left = width * item.tile.col
    val top = height * item.tile.row

    drawRect(
        rect = Rect(left, top, left + width, top + height),
        color = color.copy(alpha = opacity.toFloat()),
        antiAlias = false,
    )
}

private fun GraphiteEncoder.drawFeatures(item: GraphiteVectorRenderItem) {
    val optimizedTile = item.tile.optimizedTile ?: return
    val key = item.recordingKey
    val tileTransform = resolveGraphiteTileTransform(
        tileSizePx = Size(key.tileWidth, key.tileHeight),
        currentZoom = key.currentZoom,
        tile = item.tile,
        positionOffset = Offset.Zero,
    ) ?: return

    withTransform(GraphiteTransform.translation(tileTransform.left, tileTransform.top)) {
        withTransform(
            GraphiteTransform.scale(
                x = tileTransform.scaleX,
                y = tileTransform.scaleY,
            )
        ) {
            withClip(Rect(0f, 0f, tileTransform.extent, tileTransform.extent)) {
                optimizedTile.layerFeatures[item.batch.styleLayer.id]?.forEach { feature ->
                    when (val geometry = feature.geometry) {
                        is OptimizedGeometry.Polygon -> drawFill(
                            geometry = geometry,
                            properties = feature.properties,
                            layer = item.batch.styleLayer,
                            zoom = key.styleZoom,
                        )

                        is OptimizedGeometry.LineString -> drawLine(
                            geometry = geometry,
                            properties = feature.properties,
                            layer = item.batch.styleLayer,
                            zoom = key.styleZoom,
                            scaleAdjustment = tileTransform.scaleAdjustment,
                        )

                        is OptimizedGeometry.Point -> Unit
                    }
                }
            }
        }
    }
}

private fun GraphiteEncoder.drawFill(
    geometry: OptimizedGeometry.Polygon,
    properties: Map<String, Any>,
    layer: OptimizedStyleLayer,
    zoom: Double,
) {
    val color = layer.paint.properties["fill-color"]
        ?.evaluate(zoom, properties, layer.id) as? Color ?: Color.Magenta
    val opacity = layer.paint.properties["fill-opacity"]
        ?.evaluate(zoom, properties, layer.id) as? Double ?: 1.0
    val outlineColor = layer.paint.properties["fill-outline-color"]
        ?.evaluate(zoom, properties, layer.id) as? Color

    geometry.paths.forEach { path ->
        drawPath(path, color.copy(alpha = opacity.toFloat()))
        outlineColor?.let { outline ->
            drawPath(
                path = path,
                color = outline,
                style = GraphiteDrawStyle.Stroke(),
            )
        }
    }
}

private fun GraphiteEncoder.drawLine(
    geometry: OptimizedGeometry.LineString,
    properties: Map<String, Any>,
    layer: OptimizedStyleLayer,
    zoom: Double,
    scaleAdjustment: Float,
) {
    val color = layer.paint.properties["line-color"]
        ?.evaluate(zoom, properties, layer.id) as? Color ?: Color.Magenta
    val width = layer.paint.properties["line-width"]
        ?.evaluate(zoom, properties, layer.id) as? Double ?: 1.0
    val opacity = layer.paint.properties["line-opacity"]
        ?.evaluate(zoom, properties, layer.id) as? Double ?: 1.0
    val cap = layer.layout.properties["line-cap"]
        ?.evaluate(zoom, properties, layer.id) as? String
    val join = layer.layout.properties["line-join"]
        ?.evaluate(zoom, properties, layer.id) as? String
    val miterLimit = layer.layout.properties["line-miter-limit"]
        ?.evaluate(zoom, properties, layer.id) as? Double ?: 2.0

    drawPath(
        path = geometry.path,
        color = color.copy(alpha = opacity.toFloat()),
        style = GraphiteDrawStyle.Stroke(
            width = width.toFloat() * scaleAdjustment,
            cap = when (cap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            },
            join = when (join) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            },
            miter = miterLimit.toFloat(),
        ),
    )
}
