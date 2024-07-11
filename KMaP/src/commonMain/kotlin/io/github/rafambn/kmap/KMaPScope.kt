package io.github.rafambn.kmap

import androidx.compose.runtime.Composable
import io.github.rafambn.kmap.core.Placer
import io.github.rafambn.kmap.model.ResultTile

class KMaPConfig {
    val canvas: MutableList<Pair<Placer, suspend (zoom: Int, row: Int, column: Int) -> ResultTile>> = mutableListOf()
    val placers: MutableList<Pair<Placer, @Composable (Placer) -> Unit>> = mutableListOf()
    val clusters: MutableList<Pair<Placer, @Composable (Placer) -> Unit>> = mutableListOf()

    fun markers(items: List<Placer>, markerContent: @Composable (Placer) -> Unit) {
        items.forEach {
            placers.add(Pair(it, markerContent))
        }
    }

    fun canvas(item: Placer, getTile: suspend (zoom: Int, row: Int, column: Int) -> ResultTile) {
        canvas.add(Pair(item, getTile))
    }

    fun cluster(item: Placer, markerContent: @Composable (Placer) -> Unit) {
        clusters.add(Pair(item, markerContent))
    }
}