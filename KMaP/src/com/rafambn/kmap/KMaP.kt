package com.rafambn.kmap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.internal.rememberComponentMeasurePolicy
import com.rafambn.kmap.components.internal.rememberComponentProviderLambda

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    mapState: MapState,
    modifier: Modifier = Modifier,
    content: KMaPContent.() -> Unit,
) {
    val componentProvider = rememberComponentProviderLambda(content, mapState)

    val measurePolicy = rememberComponentMeasurePolicy(
        componentProviderLambda = componentProvider,
        mapState = mapState,
    )

    LazyLayout(
        itemProvider = componentProvider,
        modifier = modifier.clipToBounds(),
        measurePolicy = measurePolicy,
    )
}
