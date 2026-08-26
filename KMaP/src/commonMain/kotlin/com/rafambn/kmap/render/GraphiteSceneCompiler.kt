package com.rafambn.kmap.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.mapSource.tiled.ActiveTiles

internal fun compileGraphiteMapScene(content: KMaPContent): GraphiteMapScene {
    val mapState = content.mapState
    val cameraState = mapState.cameraState
    val drawReference = mapState.drawReference()
    val tileSize = mapState.drawTileSize()

    val camera = GraphiteMapCamera(
        canvasSize = IntSize(cameraState.canvasSize.xInt, cameraState.canvasSize.yInt),
        translation = mapState.drawTranslation(),
        rotationDegrees = mapState.drawRotationDegrees(),
        magnifierScale = mapState.drawMagScale(),
        positionOffset = Offset(drawReference.x.toFloat(), drawReference.y.toFloat()),
        tileSizePx = Size(
            width = with(mapState) { tileSize.width.toPx() },
            height = with(mapState) { tileSize.height.toPx() },
        ),
        zoom = cameraState.zoom.toDouble(),
    )

    val canvases = content.canvas.mapIndexed { declarationIndex, canvas ->
        val parameters = canvas.parameters as VectorCanvasParameters
        val activeTiles = mapState.canvasKernel.getActiveTiles(parameters.id)
        GraphiteVectorCanvas(
            id = parameters.id,
            declarationIndex = declarationIndex,
            zIndex = parameters.zIndex,
            style = parameters.style,
            activeTiles = ActiveTiles(
                currentZoom = activeTiles.currentZoom,
                tiles = activeTiles.tiles.toList(),
            ),
        )
    }.sortedWith(compareBy(GraphiteVectorCanvas::zIndex, GraphiteVectorCanvas::declarationIndex))

    return GraphiteMapScene(camera, canvases)
}

internal fun GraphiteMapScene.vectorBatches(): List<GraphiteVectorBatch> = buildList {
    canvases.forEachIndexed { canvasOrder, canvas ->
        val backgroundIndex = canvas.style.layers.indexOfFirst { it.type == "background" }
        if (backgroundIndex >= 0) {
            add(
                GraphiteVectorBatch(
                    canvasId = canvas.id,
                    canvasOrder = canvasOrder,
                    styleLayerIndex = backgroundIndex,
                    styleLayer = canvas.style.layers[backgroundIndex],
                    activeTiles = canvas.activeTiles,
                )
            )
        }

        canvas.style.layers.forEachIndexed { styleLayerIndex, styleLayer ->
            if (styleLayer.type == "background") return@forEachIndexed
            add(
                GraphiteVectorBatch(
                    canvasId = canvas.id,
                    canvasOrder = canvasOrder,
                    styleLayerIndex = styleLayerIndex,
                    styleLayer = styleLayer,
                    activeTiles = canvas.activeTiles,
                )
            )
        }
    }
}
