package com.rafambn.kmap.render

import com.rafambn.kmap.mapSource.tiled.tiles.OptimizedVectorTile

internal data class GraphiteVectorRenderItem(
    val batch: GraphiteVectorBatch,
    val tile: OptimizedVectorTile,
    val recordingKey: GraphiteVectorRecordingKey,
)

internal fun GraphiteMapScene.vectorRenderItems(): List<GraphiteVectorRenderItem> = buildList {
    vectorBatches().forEach { batch ->
        batch.activeTiles.tiles.forEach { tile ->
            tile as OptimizedVectorTile
            add(
                GraphiteVectorRenderItem(
                    batch = batch,
                    tile = tile,
                    recordingKey = GraphiteVectorRecordingKey(
                        canvasId = batch.canvasId,
                        styleLayerIndex = batch.styleLayerIndex,
                        styleLayer = batch.styleLayer,
                        tile = tile,
                        currentZoom = batch.activeTiles.currentZoom,
                        styleZoom = camera.zoom,
                        tileWidth = camera.tileSizePx.width,
                        tileHeight = camera.tileSizePx.height,
                    ),
                )
            )
        }
    }
}
