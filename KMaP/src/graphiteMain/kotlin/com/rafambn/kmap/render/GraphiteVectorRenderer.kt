package com.rafambn.kmap.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathSegment
import com.rafambn.graphitesurface.GraphiteEncoder
import com.rafambn.graphitesurface.GraphiteColor
import com.rafambn.graphitesurface.GraphitePaint
import com.rafambn.graphitesurface.GraphitePath
import com.rafambn.graphitesurface.GraphitePresentationInfo
import com.rafambn.graphitesurface.GraphitePresentResult
import com.rafambn.graphitesurface.GraphiteRecording
import com.rafambn.graphitesurface.GraphiteRect
import com.rafambn.graphitesurface.GraphiteRuntime
import com.rafambn.graphitesurface.GraphiteTransform
import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile
import com.rafambn.kmap.utils.style.OptimizedStyleLayer
import com.rafambn.kmap.utils.vectorTile.OptimizedGeometry
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class GraphiteVectorRenderer(
    private val runtime: GraphiteRuntime,
) {
    suspend fun render(
        scene: GraphiteMapScene,
        presentation: GraphitePresentationInfo,
    ) {
        val batches = scene.vectorBatches()
        val recordings = recordBatches(scene.camera, batches, presentation)
        try {
            val frame = runtime.createFrame(presentation) {
                recordings.inVisualOrder().forEach { (_, recording) ->
                    insert(recording)
                }
            }
            try {
                when (runtime.present(frame)) {
                    GraphitePresentResult.Accepted,
                    GraphitePresentResult.ReplacedPending,
                    GraphitePresentResult.NoPresentation,
                    GraphitePresentResult.StalePresentation -> Unit

                    GraphitePresentResult.RuntimeUnavailable ->
                        error("Graphite runtime became unavailable while presenting a map frame")
                }
            } finally {
                frame.close()
            }
        } finally {
            recordings.forEach { (_, recording) -> recording.close() }
        }
    }

    private suspend fun recordBatches(
        camera: GraphiteMapCamera,
        batches: List<GraphiteVectorBatch>,
        presentation: GraphitePresentationInfo,
    ): List<Pair<Int, GraphiteRecording>> = coroutineScope {
        val target = runtime.createRecordingTarget(presentation.pixelSize)
        batches.withIndex()
            .groupBy { (_, batch) -> recorderIndex(batch) }
            .map { (recorderIndex, indexedBatches) ->
                async {
                    indexedBatches.map { (batchIndex, batch) ->
                        batchIndex to runtime.recorders[recorderIndex].record(target) {
                            drawBatch(camera, batch)
                        }
                    }
                }
            }
            .awaitAll()
            .flatten()
    }

    private fun recorderIndex(batch: GraphiteVectorBatch): Int =
        (batch.canvasId * 31 + batch.styleLayerIndex).mod(runtime.recorders.size)
}

private fun GraphiteEncoder.drawBatch(
    camera: GraphiteMapCamera,
    batch: GraphiteVectorBatch,
) {
    val magnifierScale = 2f.pow(camera.magnifierScale)
    withTransform(GraphiteTransform.translation(camera.translation.x, camera.translation.y)) {
        withTransform(GraphiteTransform.rotationDegrees(camera.rotationDegrees)) {
            withTransform(GraphiteTransform.scale(magnifierScale)) {
                if (batch.styleLayer.type == "background") {
                    drawBackground(camera, batch)
                } else {
                    drawFeatures(camera, batch)
                }
            }
        }
    }
}

private fun GraphiteEncoder.drawBackground(
    camera: GraphiteMapCamera,
    batch: GraphiteVectorBatch,
) {
    val layer = batch.styleLayer
    val color = layer.paint.properties["background-color"]
        ?.evaluate(camera.zoom, emptyMap(), "") as? Color ?: Color.Magenta
    val opacity = layer.paint.properties["background-opacity"]
        ?.evaluate(camera.zoom, emptyMap(), "") as? Number ?: 1f

    batch.activeTiles.tiles.forEach { tile ->
        val scaleAdjustment = 2f.pow(batch.activeTiles.currentZoom - tile.zoom)
        val tileLeft = camera.tileSizePx.width * tile.col * scaleAdjustment + camera.positionOffset.x
        val tileTop = camera.tileSizePx.height * tile.row * scaleAdjustment + camera.positionOffset.y
        drawRect(
            rect = GraphiteRect(
                left = tileLeft,
                top = tileTop,
                right = tileLeft + camera.tileSizePx.width * scaleAdjustment,
                bottom = tileTop + camera.tileSizePx.height * scaleAdjustment,
            ),
            paint = GraphitePaint(
                color = color.copy(alpha = opacity.toFloat()).toGraphiteColor(),
                antiAlias = false,
            ),
        )
    }
}

private fun GraphiteEncoder.drawFeatures(
    camera: GraphiteMapCamera,
    batch: GraphiteVectorBatch,
) {
    batch.activeTiles.tiles.forEach { tile ->
        tile as OptimizedVectorTile
        val optimizedTile = tile.optimizedTile ?: return@forEach
        val tileTransform = resolveGraphiteTileTransform(
            camera = camera,
            currentZoom = batch.activeTiles.currentZoom,
            tile = tile,
        ) ?: return@forEach

        withTransform(GraphiteTransform.translation(tileTransform.left, tileTransform.top)) {
            withTransform(
                GraphiteTransform.scale(
                    x = tileTransform.scaleX,
                    y = tileTransform.scaleY,
                )
            ) {
                withClip(GraphiteRect(0f, 0f, tileTransform.extent, tileTransform.extent)) {
                    optimizedTile.layerFeatures[batch.styleLayer.id]?.forEach { feature ->
                        when (val geometry = feature.geometry) {
                            is OptimizedGeometry.Polygon -> drawFill(
                                geometry = geometry,
                                properties = feature.properties,
                                layer = batch.styleLayer,
                                zoom = camera.zoom,
                            )

                            is OptimizedGeometry.LineString -> drawLine(
                                geometry = geometry,
                                properties = feature.properties,
                                layer = batch.styleLayer,
                                zoom = camera.zoom,
                                scaleAdjustment = tileTransform.scaleAdjustment,
                            )

                            is OptimizedGeometry.Point -> Unit
                        }
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
        val graphitePath = path.toGraphitePath()
        drawPath(
            graphitePath,
            GraphitePaint(color.copy(alpha = opacity.toFloat()).toGraphiteColor()),
        )
        outlineColor?.let { outline ->
            drawPath(
                graphitePath,
                GraphitePaint(
                    color = outline.toGraphiteColor(),
                    style = GraphitePaint.Style.Stroke,
                ),
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

    drawPath(
        path = geometry.path.toGraphitePath(),
        paint = GraphitePaint(
            color = color.copy(alpha = opacity.toFloat()).toGraphiteColor(),
            style = GraphitePaint.Style.Stroke,
            strokeWidth = width.toFloat() * scaleAdjustment,
        ),
    )
}

private fun Path.toGraphitePath(): GraphitePath = GraphitePath.build {
    for (segment in this@toGraphitePath) {
        when (segment.type) {
            PathSegment.Type.Move ->
                moveTo(segment.points[0], segment.points[1])

            PathSegment.Type.Line ->
                lineTo(segment.points[2], segment.points[3])

            PathSegment.Type.Close -> close()
            PathSegment.Type.Done -> Unit
            PathSegment.Type.Quadratic,
            PathSegment.Type.Conic,
            PathSegment.Type.Cubic ->
                error("Graphite vector tiles only support line-based paths")
        }
    }
}

private fun Color.toGraphiteColor(): GraphiteColor = GraphiteColor.rgba(
    red = (red * 255).roundToInt(),
    green = (green * 255).roundToInt(),
    blue = (blue * 255).roundToInt(),
    alpha = (alpha * 255).roundToInt(),
)
