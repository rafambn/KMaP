package io.github.rafambn.kmap

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import io.github.rafambn.kmap.gestures.detectMapGestures

@Composable
internal fun CameraState(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    var topLeft = mutableStateOf(Offset(0F, 0F))
    var scale = mutableStateOf(1F)
    var angle = mutableStateOf(0F)
    val flingSpec = rememberSplineBasedDecay<Offset>()


    Layout(
        content = content,
        modifier
            .background(Color.Gray)
            .clipToBounds()
            .fillMaxSize()
            .pointerInput(true) {
                detectMapGestures(
                    onTap = { offset -> println("onTap $offset") },
                    onDoubleTap = { offset -> scale.value /= 2 },
                    onTwoFingersTap = { offset -> scale.value *= 2 },
                    onLongPress = { offset -> println("onLongPress $offset") },
                    onTapLongPress = { offset -> topLeft.value += offset },
                    onTapSwipe = { offset -> scale.value += offset.x / 7 },
                    onGesture = { centroid, pan, zoom, rotation ->
                        angle.value -= rotation
                        scale.value *= zoom
                        pan?.let { topLeft.value += pan }
                    },
                    onDrag = { dragAmount -> topLeft.value += dragAmount },
                    onDragStart = { offset -> println("onDragStart $offset") },
                    onDragEnd = { println("onDragEnd") },
                    onFling = { velocity -> println("onFling $velocity") },
                    onFlingZoom = { velocity -> println("onFlingZoom $velocity") },
                    onFlingRotation = { velocity -> println("onFlingRotation $velocity") },
                    onHover = { offset -> },
                    onScroll = { offset -> scale.value -= offset.y / 5 }
                )
            }

    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelativeWithLayer(x = topLeft.value.x.toInt(), y = topLeft.value.y.toInt()){
                    rotationZ = angle.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
            }
        }
    }
}