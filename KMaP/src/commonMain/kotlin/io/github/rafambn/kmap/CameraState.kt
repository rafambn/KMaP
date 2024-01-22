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
        println(_rawPosition.value)
        _rawPosition.value += rotateVector(offset,-angleRadian.toFloat())
    }

    fun scale(offset: Offset, scale: Float) {
        if (_zoom.value + scale < 1F)
            return
        _zoom.value += scale
        val rotatedOffset = rotateVector(offset,-angleRadian.toFloat())
        _rawPosition.value = rotatedOffset + ((_rawPosition.value - rotatedOffset) * _zoom.value / (_zoom.value - scale))
    }

    fun rotate(offset: Offset, angle: Float) {
        if (offset != Offset.Zero) {
            _angleDegres.value += angle
            angleRadian = _angleDegres.value * PI / 180

//            val tempRadianAngle = angle * PI / 180
//            val tempRadianAngTotal = angleRadian
//
//            val rotatedPosition = rotateVector(-offset, -tempRadianAngle.toFloat())
//            println("raw = ${_rawPosition.value}")
//            println("offset = $offset")
//            println("rotatedPosition = $rotatedPosition")
//            println("angle = ${_angleDegres.value}")
//            _rawPosition.value = rotatedPosition
//            println("post raw = ${_rawPosition.value}")
        }
    }

    private fun rotateVector(offset: Offset, angle: Float): Offset{
        return Offset(
            (offset.x * cos(angle) - offset.y * sin(angle)),
            (offset.x * sin(angle) + offset.y * cos(angle))
        )
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