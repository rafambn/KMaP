package io.github.rafambn.kmap.states

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CameraState(
    initialPosition: Offset = Offset.Zero,
    initialZoom: Float = 1F,
    initialRotation: Float = 0F
) {
    internal var mapSize: IntSize = IntSize.Zero

    internal val tileSize = 256F
    internal val zoom = mutableStateOf(initialZoom)
    internal val angleDegres = mutableStateOf(initialRotation)
    internal val rawPosition = mutableStateOf(initialPosition)

    private var angleRadian = 0F

    fun move(offset: Offset) {
        rawPosition.value += offset
    }

    fun scale(offset: Offset, scale: Float) {
        val previousZoom = zoom.value
        zoom.value = (scale + zoom.value).coerceIn(1F, 21F)
        rawPosition.value = offset + ((rawPosition.value - offset) * zoom.value / previousZoom)
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            val tempRadianAngle = (angle * PI / 180).toFloat()
            angleDegres.value += angle
            angleRadian = (angleDegres.value * PI / 180).toFloat()
            rawPosition.value = rotateVector(rawPosition.value - offset, tempRadianAngle) + offset
        }
    }

    private fun rotateVector(offset: Offset, angle: Float): Offset {
        return Offset(
            (offset.x * cos(angle) - offset.y * sin(angle)),
            (offset.x * sin(angle) + offset.y * cos(angle))
        )
    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: CameraState.() -> Unit = {}
): CameraState = remember {
    CameraState().apply(init)
}