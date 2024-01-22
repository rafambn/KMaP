package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class CameraState(
    initialPosition: Offset = Offset.Zero,
    zoom: Float = 1F,
    rotation: Float = 0F
) {
    var mapSize: IntSize = IntSize.Zero
        set(value) {
            _tileSize.value = minOf(value.height, value.width).toFloat()
            field = value
        }

    internal val _tileSize = mutableStateOf(minOf(mapSize.height, mapSize.width).toFloat())
    internal val _zoom = mutableStateOf(zoom)
    internal val _angleDegres = mutableStateOf(rotation)
    internal val _rawPosition = mutableStateOf(initialPosition)

    var angleRadian = -_angleDegres.value * PI / 180

    fun move(offset: Offset) {
        _rawPosition.value += Offset(
            (offset.x * cos(angleRadian) - offset.y * sin(angleRadian)).toFloat(),
            (offset.x * sin(angleRadian) + offset.y * cos(angleRadian)).toFloat()
        )
    }

    fun scale(offset: Offset, scale: Float) {
        _zoom.value += scale
        val rotatedOffset = Offset(
            (offset.x * cos(angleRadian) - offset.y * sin(angleRadian)).toFloat(),
            (offset.x * sin(angleRadian) + offset.y * cos(angleRadian)).toFloat()
        )
        _rawPosition.value = rotatedOffset + ((_rawPosition.value - rotatedOffset) * _zoom.value / (_zoom.value - scale))
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            _angleDegres.value += angle
            angleRadian = -_angleDegres.value * PI / 180

            val tempRadianAngle = angle * PI / 180
            val offsetAngle = atan(offset.y / offset.x)

//            _rawPosition.value += offset -
//                    Offset(
//                        offset.getDistance() * cos(tempRadianAngle + offsetAngle).toFloat(),
//                        offset.getDistance() * sin(tempRadianAngle + offsetAngle).toFloat()
//                    )
        }
    }

//    private fun Offset.toRaw(mapIntSize: IntSize): Offset {
//        return this - Offset(_tileSize.value * zoom / 2, _tileSize.value * zoom / 2) + Offset(
//            (mapIntSize.width / 2).toFloat(),
//            (mapIntSize.height / 2).toFloat()
//        )
//    }
//
//    private fun Offset.toLatLong(mapIntSize: IntSize): Offset {
//        return this + Offset(_tileSize.value * zoom / 2, _tileSize.value * zoom / 2) - Offset(
//            (mapIntSize.width / 2).toFloat(),
//            (mapIntSize.height / 2).toFloat()
//        )
//    }
}

@Composable
inline fun rememberCameraState(
    crossinline init: CameraState.() -> Unit = {}
): CameraState = remember {
    CameraState().apply(init)
}