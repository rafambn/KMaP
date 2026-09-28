package com.rafambn.kmap.style.expression

import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator
import com.rafambn.kmap.style.evaluation.FeatureGeometryContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

internal fun evaluateWithin(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): Boolean {
    if (expression.size != 2) return false
    val feature = context.featureGeometry ?: return false
    val polygons = readPolygons(evaluator.evaluate(expression[1], context))
        .map { polygon -> polygon.map { ring -> ring.map { project(it, feature) } } }
    if (polygons.isEmpty()) return false

    return when (context.geometryType) {
        "Point" -> feature.paths.flatten().isNotEmpty() && feature.paths.flatten().all { coordinate ->
            val point = coordinate.first.toDouble() to coordinate.second.toDouble()
            polygons.any { pointInsidePolygon(point, it) }
        }
        "LineString" -> feature.paths.isNotEmpty() && feature.paths.all { path ->
            path.isNotEmpty() && polygons.any { polygon ->
                val points = path.map { it.first.toDouble() to it.second.toDouble() }
                points.all { pointInsidePolygon(it, polygon) } &&
                    points.zipWithNext().none { (start, end) ->
                        polygon.any { ring -> edges(ring).any { (a, b) -> segmentsIntersect(start, end, a, b) } }
                    }
            }
        }
        else -> false
    }
}

private fun readPolygons(value: Any?): List<List<List<Pair<Double, Double>>>> {
    val geometry = value as? Map<*, *> ?: return emptyList()
    return when (geometry["type"]) {
        "Feature" -> readPolygons(geometry["geometry"])
        "FeatureCollection" -> (geometry["features"] as? List<*>)?.flatMap(::readPolygons) ?: emptyList()
        "Polygon" -> listOfNotNull(readPolygon(geometry["coordinates"]))
        "MultiPolygon" -> (geometry["coordinates"] as? List<*>)?.mapNotNull(::readPolygon) ?: emptyList()
        else -> emptyList()
    }
}

private fun readPolygon(value: Any?): List<List<Pair<Double, Double>>>? {
    val rings = value as? List<*> ?: return null
    val parsed = rings.map { ringValue ->
        val coordinates = ringValue as? List<*> ?: return null
        coordinates.map { coordinateValue ->
            val coordinate = coordinateValue as? List<*> ?: return null
            val longitude = (coordinate.getOrNull(0) as? Number)?.toDouble() ?: return null
            val latitude = (coordinate.getOrNull(1) as? Number)?.toDouble() ?: return null
            longitude to latitude
        }
    }
    return parsed.takeIf { it.isNotEmpty() && it.all { ring -> ring.size >= 4 } }
}

private fun project(point: Pair<Double, Double>, tile: FeatureGeometryContext): Pair<Double, Double> {
    val world = 2.0.pow(tile.zoom)
    val latitude = point.second.coerceIn(-85.05112878, 85.05112878) * PI / 180.0
    val worldX = (point.first + 180.0) / 360.0 * world
    val worldY = (1.0 - ln(tan(latitude) + 1.0 / cos(latitude)) / PI) / 2.0 * world
    return (worldX - tile.col) * tile.extent to (worldY - tile.row) * tile.extent
}

private fun pointInsidePolygon(point: Pair<Double, Double>, polygon: List<List<Pair<Double, Double>>>): Boolean {
    if (!pointInsideRing(point, polygon.first())) return false
    return polygon.drop(1).none { pointInsideRing(point, it) || pointOnRing(point, it) }
}

private fun pointInsideRing(point: Pair<Double, Double>, ring: List<Pair<Double, Double>>): Boolean {
    if (pointOnRing(point, ring)) return false
    var inside = false
    for ((a, b) in edges(ring)) {
        if ((a.second > point.second) != (b.second > point.second) &&
            point.first < (b.first - a.first) * (point.second - a.second) / (b.second - a.second) + a.first
        ) inside = !inside
    }
    return inside
}

private fun pointOnRing(point: Pair<Double, Double>, ring: List<Pair<Double, Double>>): Boolean =
    edges(ring).any { (a, b) -> pointOnSegment(point, a, b) }

private fun edges(ring: List<Pair<Double, Double>>): List<Pair<Pair<Double, Double>, Pair<Double, Double>>> =
    ring.zipWithNext() + (ring.last() to ring.first())

private fun pointOnSegment(point: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Boolean {
    val cross = (point.first - a.first) * (b.second - a.second) - (point.second - a.second) * (b.first - a.first)
    return abs(cross) < 1e-7 &&
        point.first in (minOf(a.first, b.first) - 1e-7)..(maxOf(a.first, b.first) + 1e-7) &&
        point.second in (minOf(a.second, b.second) - 1e-7)..(maxOf(a.second, b.second) + 1e-7)
}

private fun segmentsIntersect(
    a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>, d: Pair<Double, Double>
): Boolean {
    fun cross(p: Pair<Double, Double>, q: Pair<Double, Double>, r: Pair<Double, Double>): Double =
        (q.first - p.first) * (r.second - p.second) - (q.second - p.second) * (r.first - p.first)
    val abC = cross(a, b, c)
    val abD = cross(a, b, d)
    val cdA = cross(c, d, a)
    val cdB = cross(c, d, b)
    return (abs(abC) < 1e-7 && pointOnSegment(c, a, b)) ||
        (abs(abD) < 1e-7 && pointOnSegment(d, a, b)) ||
        (abs(cdA) < 1e-7 && pointOnSegment(a, c, d)) ||
        (abs(cdB) < 1e-7 && pointOnSegment(b, c, d)) ||
        (abC > 0) != (abD > 0) && (cdA > 0) != (cdB > 0)
}
