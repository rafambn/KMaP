package com.rafambn.kmap.render

import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.utils.style.OptimizedStyleLayer

internal fun KMaPContent.graphiteIncompatibility(): String? {
    if (canvas.isEmpty()) return "Graphite requires at least one canvas"

    canvas.firstOrNull { it.parameters is RasterCanvasParameters }?.let {
        return "Graphite does not render maps containing rasterCanvas"
    }

    canvas.firstOrNull { it.parameters !is VectorCanvasParameters }?.let { canvas ->
        return "Graphite requires vector canvases: ${canvas.parameters.id}"
    }

    if (markers.isNotEmpty()) return "Graphite does not render markers yet"
    if (cluster.isNotEmpty()) return "Graphite does not render clusters yet"
    if (paths.isNotEmpty()) return "Graphite does not render paths yet"

    canvas.forEach { canvas ->
        if (canvas.parameters.alpha != 1f) {
            return "Graphite requires vector canvas alpha == 1: ${canvas.parameters.id}"
        }
    }

    canvas.forEach { canvas ->
        val parameters = canvas.parameters as VectorCanvasParameters
        parameters.style.layers.firstOrNull { it.type == "symbol" }?.let { layer ->
            return "Graphite does not render vector symbol layers yet: ${layer.id}"
        }
    }

    canvas.forEach { canvas ->
        val parameters = canvas.parameters as VectorCanvasParameters
        parameters.style.layers.firstOrNull { it.type !in supportedLayerTypes }?.let { layer ->
            return "Graphite does not render vector layer type '${layer.type}' yet: ${layer.id}"
        }
    }

    canvas.forEach { canvas ->
        val parameters = canvas.parameters as VectorCanvasParameters
        parameters.style.layers.forEach { layer ->
            layer.unsupportedGraphiteProperty()?.let { property ->
                return "Graphite does not render property '$property' yet: ${layer.id}"
            }
        }
    }

    return null
}

private fun OptimizedStyleLayer.unsupportedGraphiteProperty(): String? {
    val supportedLayout = when (type) {
        "line" -> setOf("line-cap", "line-join")
        else -> emptySet()
    }
    val supportedPaint = when (type) {
        "background" -> setOf("background-color", "background-opacity")
        "fill" -> setOf("fill-color", "fill-opacity", "fill-outline-color")
        "line" -> setOf("line-color", "line-width", "line-opacity")
        else -> emptySet()
    }

    return layout.properties.keys.firstOrNull { it !in supportedLayout }
        ?: paint.properties.keys.firstOrNull { it !in supportedPaint }
}

private val supportedLayerTypes = setOf("background", "fill", "line")
