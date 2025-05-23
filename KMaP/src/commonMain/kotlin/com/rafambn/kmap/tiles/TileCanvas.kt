package com.rafambn.kmap.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rafambn.kmap.components.CanvasComponent
import com.rafambn.kmap.core.CameraState
import com.rafambn.kmap.core.ViewPort
import com.rafambn.kmap.mapProperties.MapProperties
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.asOffset
import com.rafambn.kmap.utils.loopInZoom
import com.rafambn.kmap.utils.toIntFloor
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
internal fun TileCanvas(
    cameraState: CameraState,
    mapProperties: MapProperties,
    positionOffset: CanvasDrawReference,
    viewPort: ViewPort,
    modifier: Modifier,
    canvasComponent: CanvasComponent,
) {
    val zoomLevel = cameraState.zoom.toIntFloor()
    val magnifierScale = cameraState.zoom - zoomLevel
    val tileSize = mapProperties.tileSize
    val rotationDegrees = cameraState.angleDegrees.toFloat()
    val translation = cameraState.canvasSize.asOffset() / 2F
    val visibleTiles = TileFinder().getVisibleTilesForLevel(
        viewPort,
        zoomLevel,
        mapProperties.outsideTiles,
        mapProperties.tileSize
    )
    val coroutineScope = rememberCoroutineScope()
    var tileLayers = remember { TileLayers() }
    val canvasState = remember { TileRenderer(canvasComponent.getTile, canvasComponent.maxCacheTiles, coroutineScope) }

    val renderedTilesCache = canvasState.renderedTilesFlow.collectAsState()
    if (zoomLevel != tileLayers.frontLayer.level)
        tileLayers.changeLayer(zoomLevel)

    val newFrontLayer = mutableListOf<Tile>()
    val tilesToRender = mutableListOf<TileSpecs>()
    visibleTiles.forEach { tileSpecs ->
        val foundInFrontLayer = tileLayers.frontLayer.tiles.find { it == tileSpecs }
        val foundInRenderedTiles = renderedTilesCache.value.find {
            it == TileSpecs(
                tileSpecs.zoom,
                tileSpecs.row.loopInZoom(tileSpecs.zoom),
                tileSpecs.col.loopInZoom(tileSpecs.zoom)
            )
        }

        when {
            foundInFrontLayer != null -> {
                newFrontLayer.add(foundInFrontLayer)
            }

            foundInRenderedTiles != null -> {
                newFrontLayer.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, foundInRenderedTiles.imageBitmap))
            }

            else -> {
                newFrontLayer.add(Tile(tileSpecs.zoom, tileSpecs.row, tileSpecs.col, null))
                tilesToRender.add(tileSpecs)
            }
        }
    }
    tileLayers.updateFrontLayerTiles(newFrontLayer)
    coroutineScope.launch { canvasState.renderTiles(tilesToRender) }
    renderedTilesCache.value.forEach {
        tileLayers.insertNewTileBitmap(it)
    }
    Layout(
        modifier = modifier
            .then(canvasComponent.gestureDetector?.let { Modifier.pointerInput(PointerEventPass.Main) { it(this) } } ?: Modifier)
            .graphicsLayer {
                alpha = canvasComponent.alpha
                clip = true
            }
            .zIndex(canvasComponent.zIndex)
            .drawBehind {
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotationDegrees, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawTiles(
                            tileLayers.backLayer.tiles,
                            tileSize,
                            positionOffset,
                            2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level),
                            canvas
                        )
                        drawTiles(
                            tileLayers.frontLayer.tiles,
                            tileSize,
                            positionOffset,
                            1F,
                            canvas
                        )
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun DrawScope.drawTiles(tiles: List<Tile>, tileSize: TileDimension, positionOffset: CanvasDrawReference, scaleAdjustment: Float = 1F, canvas: Canvas) {
    tiles.forEach { tile ->
        tile.imageBitmap?.let {
            canvas.drawImageRect(
                image = it,
                dstOffset = IntOffset(
                    (tileSize.width * tile.row * scaleAdjustment + positionOffset.horizontal).dp.toPx().toIntFloor(),
                    (tileSize.height * tile.col * scaleAdjustment + positionOffset.vertical).dp.toPx().toIntFloor()
                ),
                dstSize = IntSize(
                    (tileSize.width.dp.toPx() * scaleAdjustment).toIntFloor(),
                    (tileSize.height.dp.toPx() * scaleAdjustment).toIntFloor()
                ),
                paint = Paint().apply {
                    isAntiAlias = false
                    filterQuality = FilterQuality.High
                }
            )
        }
    }
}