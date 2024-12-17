package com.rafambn.kmap.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rafambn.kmap.components.Parameters
import com.rafambn.kmap.core.MapState

@Composable
fun rememberComponentProviderLambda(content: KMaPScope.() -> Unit, mapState: MapState): () -> ComponentProvider {
    val latestContent = rememberUpdatedState(content)

    return remember(mapState) {
        val kMaPContentState = derivedStateOf(referentialEqualityPolicy()) {
            KMaPContent(latestContent.value)
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

    //Because you can't place a composable twice, you can't measure a cluster composable and put it in multiple places
    //but you can double the amount of composable in the lazy layout and map the second half to the cluster of the marker
    //this way if i want to to cluster indexes 5 and 7 i measure index 5*2 getting the cluster of of marker index == 5
    override val itemCount
        get() = kmapContent.markers.size * 2

    val markersCount
        get() = kmapContent.markers.size

    @Composable
    override fun Item(index: Int, key: Any) {
        val item = kmapContent.markers.getOrNull(index)
        if (item == null) {
            val cluster = kmapContent.cluster.find { it.clusterParameters.id == kmapContent.markers[index / 2].markerParameters.clusterId }
            cluster?.clusterContent()
        } else
            item.markerContent()
    }

    val canvasList get() = kmapContent.canvas

    val pathList get() = kmapContent.paths

    fun getParameters(index: Int): Parameters {
        val item = kmapContent.markers.getOrNull(index)
        return if (item == null)
            kmapContent.cluster.find { it.clusterParameters.id == kmapContent.markers[index / 2].markerParameters.clusterId }!!.clusterParameters
        else
            item.markerParameters
    }
}