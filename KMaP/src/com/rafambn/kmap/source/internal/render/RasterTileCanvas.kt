package com.rafambn.kmap.source.internal.render

import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.geometry.plane.CanvasDrawReference
import com.rafambn.kmap.gesture.MapGestureWrapper
import com.rafambn.kmap.gesture.internal.mapGestures
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.source.RasterTile
import com.rafambn.kmap.source.Tile
import com.rafambn.kmap.source.internal.ActiveTiles
import com.rafambn.kmap.utils.toIntFloor
import kotlin.math.pow

@Composable
fun RasterTileCanvas(
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    activeTiles: () -> ActiveTiles,
) {
    Layout(
        modifier = Modifier
            .mapGestures(gestureWrapper)
            .drawBehind {
                val rotation = rotationDegrees()
                val magnifierScale = magnifierScale()
                val tileSize = tileSize()
                val positionOffset = positionOffset()
                val activeTiles = activeTiles()
                withTransform({
                    translate(center.x, center.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        activeTiles.tiles.forEach { tile ->
                            val scaleAdjustment = 2F.pow(activeTiles.currentZoom - tile.zoom)
                            drawRasterTiles(
                                listOf(tile),
                                tileSize,
                                positionOffset,
                                scaleAdjustment,
                                canvas,
                            )
                        }
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun DrawScope.drawRasterTiles(
    tiles: List<Tile>,
    tileSize: TileDimension,
    positionOffset: CanvasDrawReference,
    scaleAdjustment: Float = 1F,
    canvas: Canvas,
) {
    tiles.forEach { tile ->
        canvas.drawImageRect(
            image = (tile as RasterTile).imageBitmap!!,
            dstOffset = IntOffset(
                (tileSize.width.toPx().toDouble() * tile.col * scaleAdjustment + positionOffset.x).toIntFloor(),
                (tileSize.height.toPx().toDouble() * tile.row * scaleAdjustment + positionOffset.y).toIntFloor()
            ),
            dstSize = IntSize(
                (tileSize.width.toPx() * scaleAdjustment).toIntFloor(),
                (tileSize.height.toPx() * scaleAdjustment).toIntFloor()
            ),
            paint = Paint().apply {
                isAntiAlias = false
                filterQuality = FilterQuality.High
            }
        )
    }
}
