package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.rafambn.kmap.lazyMarker.KMaPContent
import com.rafambn.kmap.lazyMarker.rememberComponentProviderLambda
import com.rafambn.kmap.lazyMarker.rememberComponentMeasurePolicy

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    content: KMaPContent.() -> Unit
) {
    val itemProvider = rememberComponentProviderLambda(content, mapState)

    val measurePolicy = rememberComponentMeasurePolicy(
        componentProviderLambda = itemProvider,
        mapState = mapState,
    )

    LazyLayout(
        itemProvider = itemProvider,
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                mapState.setCanvasSize(
                    Offset(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                )
            },
        prefetchState = null,
        measurePolicy = measurePolicy
    )
}
