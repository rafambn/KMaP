package com.rafambn.kmap.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rafambn.kmap.core.MapState

@Composable
fun rememberComponentProviderLambda(content: KMaPContent.() -> Unit, mapState: MapState): () -> ComponentProvider {
    val latestContent = rememberUpdatedState(content)

    return remember(mapState) {
        val kMaPContentState = derivedStateOf(referentialEqualityPolicy()) {
            KMaPContent(latestContent.value, mapState)
        }
        val kmapProviderState = derivedStateOf(referentialEqualityPolicy()) {
            ComponentProvider(kMaPContentState.value)
        }
        kmapProviderState::value
    }
}

@OptIn(ExperimentalFoundationApi::class)
class ComponentProvider(
    private val kmapContent: KMaPContent
) : LazyLayoutItemProvider {

    // Because you can't place a composable twice, you can't measure a cluster composable and put it in multiple places
    // but you can double the amount of markers in the lazy layout and map the second half to the cluster of the marker
    // this way if i want to cluster indexes 5 and 7 i measure index 5*2 getting the cluster of marker index == 5
    //
    // Array representation:
    // [0 ... markersCount-1] -> Markers
    // [markersCount ... markersCount*2-1] -> Clusters
    // [markersCount*2 ... markersCount*2+pathsCount-1] -> Paths
    // [markersCount*2+pathsCount ... markersCount*2+pathsCount+canvasCount-1] -> Canvas

    override val itemCount: Int
        get() = markersCount * 2 + pathsCount + canvasCount

    val markersCount: Int
        get() = kmapContent.markers.size

    val pathsCount: Int
        get() = kmapContent.paths.size

    val canvasCount: Int
        get() = kmapContent.canvas.size

    @Composable
    override fun Item(index: Int, key: Any) {
        when {
            // Markers area: [0 ... markersCount-1]
            index < markersCount -> {
                kmapContent.markers[index].content()
            }
            // Marker clusters area: [markersCount ... markersCount*2-1]
            index < markersCount * 2 -> {
                val markerIndex = index - markersCount
                val marker = kmapContent.markers[markerIndex]
                val cluster = kmapContent.cluster.firstOrNull() { it.parameters.id == marker.parameters.clusterId }
                cluster?.content()
            }
            // Paths area: [markersCount*2 ... markersCount*2+pathsCount-1]
            index < markersCount * 2 + pathsCount -> {
                val pathIndex = index - (markersCount * 2)
                kmapContent.paths[pathIndex].content()
            }
            // Canvas area: [markersCount*2+pathsCount ... markersCount*2+pathsCount+canvasCount-1]
            else -> {
                val canvasIndex = index - (markersCount * 2 + pathsCount)
                kmapContent.canvas[canvasIndex].content()
            }
        }
    }

    fun getParameters(index: Int): Parameters {
        return when {
            // Markers area: [0 ... markersCount-1]
            index < markersCount -> {
                kmapContent.markers[index].parameters
            }
            // Marker clusters area: [markersCount ... markersCount*2-1]
            index < markersCount * 2 -> {
                val markerIndex = index - markersCount
                val marker = kmapContent.markers[markerIndex]
                kmapContent.cluster.first { it.parameters.id == marker.parameters.clusterId }.parameters
            }
            // Paths area: [markersCount*2 ... markersCount*2+pathsCount-1]
            index < markersCount * 2 + pathsCount -> {
                val pathIndex = index - (markersCount * 2)
                kmapContent.paths[pathIndex].parameters
            }
            // Canvas area: [markersCount*2+pathsCount ... markersCount*2+pathsCount+canvasCount-1]
            else -> {
                val canvasIndex = index - (markersCount * 2 + pathsCount)
                kmapContent.canvas[canvasIndex].parameters
            }
        }
    }
}
