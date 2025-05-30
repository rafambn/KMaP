package com.rafambn.kmap.components.marker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.Parameters

@OptIn(ExperimentalFoundationApi::class)
class MarkerProvider(
    private val kmapContent: KMaPContent
) : LazyLayoutItemProvider {

    //Because you can't place a composable twice, you can't measure a cluster composable and put it in multiple places
    //but you can double the amount of composable in the lazy layout and map the second half to the cluster of the marker
    //this way if i want to to cluster indexes 5 and 7 i measure index 5*2 getting the cluster of of marker index == 5
    override val itemCount
        get() = markersCount * 2

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

    fun getParameters(index: Int): Parameters {
        val item = kmapContent.markers.getOrNull(index)
        return if (item == null)
            kmapContent.cluster.find { it.clusterParameters.id == kmapContent.markers[index / 2].markerParameters.clusterId }!!.clusterParameters
        else
            item.markerParameters
    }
}
