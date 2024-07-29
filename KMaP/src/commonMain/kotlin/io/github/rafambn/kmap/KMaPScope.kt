package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import io.github.rafambn.kmap.core.CanvasData
import io.github.rafambn.kmap.core.ClusterData
import io.github.rafambn.kmap.core.MarkerData
import io.github.rafambn.kmap.model.ResultTile

class KMaPContent : KMaPConfig {
    val canvas: MutableList<Pair<CanvasData, suspend (zoom: Int, row: Int, column: Int) -> ResultTile>> = mutableListOf()
    val markers: MutableList<Pair<MarkerData, @Composable (MarkerData) -> Unit>> = mutableListOf()
    val clusters: MutableList<Pair<ClusterData, @Composable (ClusterData) -> Unit>> = mutableListOf()

    override fun markers(items: List<MarkerData>, markerContent: @Composable (MarkerData) -> Unit) {
        items.forEach {
            markers.add(Pair(it, markerContent))
        }
    }

    override fun canvas(item: CanvasData, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) {
        canvas.add(Pair(item, getTile))
    }

    override fun cluster(item: ClusterData, markerContent: @Composable (ClusterData) -> Unit) {
        clusters.add(Pair(item, markerContent))
    }

    fun update() {

    }
}

interface KMaPConfig {
    fun markers(items: List<MarkerData>, markerContent: @Composable (MarkerData) -> Unit)

    fun canvas(item: CanvasData, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile)

    fun cluster(item: ClusterData, markerContent: @Composable (ClusterData) -> Unit)
}