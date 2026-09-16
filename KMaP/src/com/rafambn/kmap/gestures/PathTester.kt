package com.rafambn.kmap.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.sqrt

class PathTester(
    private val path: PathHitTester,
    private val pathMeasure: PathMeasure,
    private val threshold: Float,
    private val checkForInsideClick: Boolean,
    private val pathTranslation: Offset,
) {

    fun checkHit(point: Offset): Boolean {
        val translatedPoint = point.minus(Offset(threshold - pathTranslation.x, threshold - pathTranslation.y))
        if (isPointInsidePath(path, translatedPoint) && checkForInsideClick)
            return true

        if (isPointNearPath(pathMeasure, translatedPoint, threshold))
            return true

        return false
    }

    private fun isPointInsidePath(path: PathHitTester, point: Offset): Boolean {
        return path.contains(point)
    }

    private fun isPointNearPath(
        pathMeasure: PathMeasure,
        point: Offset,
        threshold: Float
    ): Boolean {
        val closestPoint = findClosestPointOnPath(pathMeasure, point)
        val distance = calculateDistance(point, closestPoint)
        return distance <= threshold
    }

    private fun findClosestPointOnPath(pathMeasure: PathMeasure, point: Offset): Offset {
        val pathLength = pathMeasure.length
        val initialPointOnPath = pathMeasure.getPosition(0f)

        if (pathLength == 0f) {
            return initialPointOnPath
        }

        val dxInitial = point.x - initialPointOnPath.x
        val dyInitial = point.y - initialPointOnPath.y
        var overallMinDistanceSquared = dxInitial * dxInitial + dyInitial * dyInitial
        var overallClosestPoint = initialPointOnPath

        val minSamples = 30
        val maxSamples = 1000
        val samplesPerUnitLength = 0.2f

        val calculatedSamples = (pathLength * samplesPerUnitLength).toInt()
        val numSamplesToProcess = calculatedSamples.coerceIn(minSamples, maxSamples)

        if (numSamplesToProcess > 0) {
            for (i in 1..numSamplesToProcess) {
                val fraction = i.toFloat() / numSamplesToProcess.toFloat()
                val currentDistanceOnPath = pathLength * fraction
                val pointOnPath = pathMeasure.getPosition(currentDistanceOnPath)

                val dx = point.x - pointOnPath.x
                val dy = point.y - pointOnPath.y
                val currentDistanceSquared = dx * dx + dy * dy

                if (currentDistanceSquared < overallMinDistanceSquared) {
                    overallMinDistanceSquared = currentDistanceSquared
                    overallClosestPoint = pointOnPath
                }
            }
        }

        return overallClosestPoint
    }

    private fun calculateDistance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
