package com.rafambn.kmap.utils

import androidx.compose.ui.geometry.Offset

open class Reference(val x: Double, val y: Double) {

    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    val xFloat: Float get() = x.toFloat()
    val yFloat: Float get() = y.toFloat()
    val xInt: Int get() = x.toInt()
    val yInt: Int get() = y.toInt()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Reference) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

    override fun toString(): String = "${this::class.simpleName}(x=$x, y=$y)"

    companion object {
        val ZERO = Reference(0.0, 0.0)
    }
}

class ScreenOffset : Reference {
    constructor(x: Double, y: Double) : super(x, y)
    constructor(x: Float, y: Float) : super(x, y)
    constructor(x: Int, y: Int) : super(x, y)

    companion object {
        val Zero = ScreenOffset(0.0, 0.0)
    }
}

class TilePoint : Reference {
    constructor(horizontal: Double, vertical: Double) : super(horizontal, vertical)
    constructor(horizontal: Float, vertical: Float) : super(horizontal, vertical)
    constructor(horizontal: Int, vertical: Int) : super(horizontal, vertical)

    companion object {
        val Zero = TilePoint(0.0, 0.0)
    }
}

class Coordinates : Reference {
    constructor(longitude: Double, latitude: Double) : super(longitude, latitude)
    constructor(longitude: Float, latitude: Float) : super(longitude, latitude)
    constructor(longitude: Int, latitude: Int) : super(longitude, latitude)

    companion object {
        val Zero = Coordinates(0.0, 0.0)
    }
}

class ProjectedCoordinates : Reference {
    constructor(longitude: Double, latitude: Double) : super(longitude, latitude)
    constructor(longitude: Float, latitude: Float) : super(longitude, latitude)
    constructor(longitude: Int, latitude: Int) : super(longitude, latitude)

    companion object {
        val Zero = ProjectedCoordinates(0.0, 0.0)
    }
}

class DifferentialScreenOffset : Reference {
    constructor(dx: Double, dy: Double) : super(dx, dy)
    constructor(dx: Float, dy: Float) : super(dx, dy)
    constructor(dx: Int, dy: Int) : super(dx, dy)

    companion object {
        val Zero = DifferentialScreenOffset(0.0, 0.0)
    }
}

class CanvasDrawReference : Reference {
    constructor(horizontal: Double, vertical: Double) : super(horizontal, vertical)
    constructor(horizontal: Float, vertical: Float) : super(horizontal, vertical)
    constructor(horizontal: Int, vertical: Int) : super(horizontal, vertical)

    companion object {
        val Zero = CanvasDrawReference(0.0, 0.0)
    }
}

inline operator fun <reified T : Reference> T.plus(other: T): T {
    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(x + other.x, y + other.y) as T
        TilePoint::class -> TilePoint(x + other.x, y + other.y) as T
        Coordinates::class -> Coordinates(x + other.x, y + other.y) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(x + other.x, y + other.y) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(x + other.x, y + other.y) as T
        CanvasDrawReference::class -> CanvasDrawReference(x + other.x, y + other.y) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

inline operator fun <reified T : Reference> T.unaryMinus(): T {
    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(-x, -y) as T
        TilePoint::class -> TilePoint(-x, -y) as T
        Coordinates::class -> Coordinates(-x, -y) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(-x, -y) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(-x, -y) as T
        CanvasDrawReference::class -> CanvasDrawReference(-x, -y) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

inline operator fun <reified T : Reference> T.minus(other: T): T {
    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(x - other.x, y - other.y) as T
        TilePoint::class -> TilePoint(x - other.x, y - other.y) as T
        Coordinates::class -> Coordinates(x - other.x, y - other.y) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(x + other.x, y + other.y) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(x - other.x, y - other.y) as T
        CanvasDrawReference::class -> CanvasDrawReference(x - other.x, y - other.y) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

inline operator fun <reified T : Reference> T.times(value: Number): T {
    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(x * value.toDouble(), y * value.toDouble()) as T
        TilePoint::class -> TilePoint(x * value.toDouble(), y * value.toDouble()) as T
        Coordinates::class -> Coordinates(x * value.toDouble(), y * value.toDouble()) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(x * value.toDouble(), y * value.toDouble()) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(x * value.toDouble(), y * value.toDouble()) as T
        CanvasDrawReference::class -> CanvasDrawReference(x * value.toDouble(), y * value.toDouble()) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

inline operator fun <reified T : Reference> T.div(value: Number): T {
    return when (T::class) {
        ScreenOffset::class -> ScreenOffset(x / value.toDouble(), y / value.toDouble()) as T
        TilePoint::class -> TilePoint(x / value.toDouble(), y / value.toDouble()) as T
        Coordinates::class -> Coordinates(x / value.toDouble(), y / value.toDouble()) as T
        ProjectedCoordinates::class -> ProjectedCoordinates(x * value.toDouble(), y * value.toDouble()) as T
        DifferentialScreenOffset::class -> DifferentialScreenOffset(x / value.toDouble(), y / value.toDouble()) as T
        CanvasDrawReference::class -> CanvasDrawReference(x / value.toDouble(), y / value.toDouble()) as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

fun TilePoint.asScreenOffset() = ScreenOffset(this.x, this.y)
fun TilePoint.asCanvasDrawReference() = CanvasDrawReference(this.x, this.y)

fun ScreenOffset.asOffset() = Offset(this.xFloat, this.yFloat)
fun ScreenOffset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)

fun Offset.asScreenOffset() = ScreenOffset(this.x, this.y)
fun Offset.asDifferentialScreenOffset() = DifferentialScreenOffset(this.x, this.y)

fun DifferentialScreenOffset.asCanvasPosition() = TilePoint(this.x, this.y)
fun DifferentialScreenOffset.asTilePoint() = TilePoint(this.x, this.y)

fun transformReference(
    pointX: Double,
    pointY: Double,
    sourceRangeX: Pair<Double, Double>,
    sourceRangeY: Pair<Double, Double>,
    targetRangeX: Pair<Double, Double>,
    targetRangeY: Pair<Double, Double>
): Pair<Double, Double> {

    val sourceWidth = sourceRangeX.second - sourceRangeX.first
    val sourceHeight = sourceRangeY.second - sourceRangeY.first
    val targetWidth = targetRangeX.second - targetRangeX.first
    val targetHeight = targetRangeY.second - targetRangeY.first

    val normalizedX = (pointX - sourceRangeX.first) / sourceWidth
    val normalizedY = (pointY - sourceRangeY.first) / sourceHeight

    val transformedX = normalizedX * targetWidth + targetRangeX.first
    val transformedY = normalizedY * targetHeight + targetRangeY.first

    return Pair(transformedX, transformedY)
}
