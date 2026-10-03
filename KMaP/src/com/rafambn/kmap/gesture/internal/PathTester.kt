package com.rafambn.kmap.gesture.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import kotlin.math.sqrt

/** Tests fill and outline in padded path-local coordinates, including every contour. */
class PathTester(
    path: Path,
    private val threshold: Float,
    private val checkForInsideClick: Boolean,
) {
    init {
        require(threshold.isFinite() && threshold >= 0f) { "Path hit threshold must be finite and non-negative" }
    }

    // Flatten curves once, using a finer approximation for smaller hit thresholds.
    private val tolerance = minOf(0.25f, maxOf(threshold / 8f, 0.001f))
    private val fill = PathHitTester(path, tolerance)
    private val translation = Offset(threshold, threshold) - path.getBounds().topLeft
    private val lines = mutableListOf<Pair<Offset, Offset>>()

    init {
        var contourStart = Offset.Zero
        var end = Offset.Zero
        // Use the buffer API: Skia's next() reports conics as quadratics in Compose 1.12.0.
        val iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)
        val p = FloatArray(8)
        while (iterator.hasNext()) {
            when (iterator.next(p)) {
                PathSegment.Type.Move -> {
                    contourStart = Offset(p[0], p[1])
                    end = contourStart
                }
                PathSegment.Type.Line -> {
                    end = Offset(p[2], p[3])
                    lines += Offset(p[0], p[1]) to end
                }
                PathSegment.Type.Quadratic -> {
                    end = Offset(p[4], p[5])
                    val start = Offset(p[0], p[1])
                    val control = Offset(p[2], p[3])
                    addCubic(start, start + (control - start) * (2f / 3f), end + (control - end) * (2f / 3f), end)
                }
                PathSegment.Type.Cubic -> {
                    end = Offset(p[6], p[7])
                    addCubic(Offset(p[0], p[1]), Offset(p[2], p[3]), Offset(p[4], p[5]), end)
                }
                PathSegment.Type.Conic -> {
                    end = Offset(p[4], p[5])
                    addConic(Offset(p[0], p[1]), Offset(p[2], p[3]), end, p[6])
                }
                PathSegment.Type.Close -> {
                    lines += end to contourStart
                    end = contourStart
                }
                PathSegment.Type.Done -> break
            }
        }
    }

    private fun addConic(start: Offset, control: Offset, end: Offset, weight: Float, depth: Int = 0) {
        if (depth == 24 || distanceSquared(control, start, end) <= tolerance * tolerance) {
            lines += start to end
            return
        }
        val a = (start + control * weight) / (1f + weight)
        val b = (end + control * weight) / (1f + weight)
        val middle = (a + b) / 2f
        val nextWeight = sqrt((1f + weight) / 2f)
        addConic(start, a, middle, nextWeight, depth + 1)
        addConic(middle, b, end, nextWeight, depth + 1)
    }

    fun checkHit(point: Offset): Boolean {
        val translated = point - translation
        if (checkForInsideClick && fill.contains(translated)) return true
        return lines.any { (start, end) -> distanceSquared(translated, start, end) <= threshold * threshold }
    }

    private fun addCubic(start: Offset, control1: Offset, control2: Offset, end: Offset, depth: Int = 0) {
        if (depth == 24 || maxOf(
                distanceSquared(control1, start, end),
                distanceSquared(control2, start, end),
            ) <= tolerance * tolerance) {
            lines += start to end
            return
        }
        val a = (start + control1) / 2f
        val b = (control1 + control2) / 2f
        val c = (control2 + end) / 2f
        val d = (a + b) / 2f
        val e = (b + c) / 2f
        val middle = (d + e) / 2f
        addCubic(start, a, d, middle, depth + 1)
        addCubic(middle, e, c, end, depth + 1)
    }

    private fun distanceSquared(point: Offset, start: Offset, end: Offset): Float {
        val segment = end - start
        val lengthSquared = segment.getDistanceSquared()
        if (lengthSquared == 0f) return (point - start).getDistanceSquared()
        val relative = point - start
        val fraction = ((relative.x * segment.x + relative.y * segment.y) / lengthSquared).coerceIn(0f, 1f)
        return (point - (start + segment * fraction)).getDistanceSquared()
    }
}
