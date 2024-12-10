package com.rafambn.kmap.lazyMarker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.MapState


@OptIn(ExperimentalFoundationApi::class)
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

    override val itemCount
        get() = kmapContent.markers.size


    @Composable
    override fun Item(index: Int, key: Any) {
        val item = kmapContent.markers.getOrNull(index)
        item?.markerContent?.invoke(item.markerParameters)
    }

    val canvasList get() = kmapContent.canvas

    fun getParameters(index: Int): MarkerParameters {
        val item = kmapContent.markers[index]
        return item.markerParameters
    }

//    fun getItemIndexesInRange(boundaries: BoundingBox): List<Int> {
//        val result = mutableListOf<Int>()
//
//        kmapContent.value.forEachIndexed { index, itemContent ->
//            val listItem = itemContent.item
//            if (listItem.x in boundaries.fromX..boundaries.toX &&
//                listItem.y in boundaries.fromY..boundaries.toY
//            ) {
//                result.add(index)
//            }
//        }
//
//        return result
//    }
//
//    fun getItem(index: Int): ListItem? {
//        return itemsState.value.getOrNull(index)?.item
//    }
}