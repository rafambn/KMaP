package com.rafambn.kmap.mapSource.tiled.raster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.gestures.MapGestureWrapper
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.gestures.sharedPointerInput
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.Tile
import com.rafambn.kmap.mapSource.tiled.VectorTile
import com.rafambn.kmap.mapSource.tiled.TileDimension
import com.rafambn.kmap.mapSource.tiled.TileLayers
import com.rafambn.kmap.utils.CanvasDrawReference
import com.rafambn.kmap.utils.ScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
import com.rafambn.kmap.utils.style.Style
import com.rafambn.kmap.utils.style.StyleLayer
import com.rafambn.kmap.utils.toIntFloor
import com.rafambn.kmap.utils.vectorTile.MVTFeature
import com.rafambn.kmap.utils.vectorTile.MVTLayer
import com.rafambn.kmap.utils.vectorTile.RawMVTGeomType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.pow

@Composable
internal fun RasterTileCanvas(
    id: Int,
    canvasSize: ScreenOffset,
    gestureWrapper: MapGestureWrapper?,
    magnifierScale: () -> Float,
    positionOffset: () -> CanvasDrawReference,
    tileSize: () -> TileDimension,
    rotationDegrees: () -> Float,
    translation: () -> Offset,
    tileLayers: (Int) -> TileLayers,
) {
    Layout(
        modifier = Modifier
            .then(gestureWrapper?.let {
                Modifier.sharedPointerInput {
                    detectMapGestures(
                        onTap = gestureWrapper.onTap,
                        onDoubleTap = gestureWrapper.onDoubleTap,
                        onLongPress = gestureWrapper.onLongPress,
                        onTapLongPress = gestureWrapper.onTapLongPress,
                        onTapSwipe = gestureWrapper.onTapSwipe,
                        onGesture = gestureWrapper.onGesture,
                        onTwoFingersTap = gestureWrapper.onTwoFingersTap,
                        onHover = gestureWrapper.onHover,
                        onScroll = null,
                    )
                }
            } ?: Modifier)
            .then(gestureWrapper?.onScroll?.let {//TODO quick fix of scroll, fix code properly
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            if (pointerEvent.type == PointerEventType.Scroll) {
                                pointerEvent.changes.forEach {
                                    if (it.scrollDelta.y != 0F)
                                        gestureWrapper.onScroll.invoke(it.position.asScreenOffset(), it.scrollDelta.y / 5)
                                }
                            }
                        }
                    }
                }
            } ?: Modifier)
            .drawBehind {
                val translation = translation()
                val rotation = rotationDegrees()
                val magnifierScale = magnifierScale()
                val tileSize = tileSize()
                val positionOffset = positionOffset()
                val tileLayers = tileLayers(id)
                withTransform({
                    translate(translation.x, translation.y)
                    rotate(rotation, Offset.Zero)
                    scale(2F.pow(magnifierScale), Offset.Zero)
                }) {
                    drawIntoCanvas { canvas ->
                        drawTiles(
                            tileLayers.backLayer.tiles,
                            tileSize,
                            positionOffset,
                            2F.pow(tileLayers.frontLayer.level - tileLayers.backLayer.level),
                            canvas,
                        )
                        drawTiles(
                            tileLayers.frontLayer.tiles,
                            tileSize,
                            positionOffset,
                            1F,
                            canvas,
                        )
                    }
                }
            }
    ) { _, _ ->
        layout(canvasSize.xInt, canvasSize.yInt) {}
    }
}

private fun DrawScope.drawTiles(
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
                (tileSize.width * tile.row * scaleAdjustment + positionOffset.x).dp.toPx().toIntFloor(),
                (tileSize.height * tile.col * scaleAdjustment + positionOffset.y).dp.toPx().toIntFloor()
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
