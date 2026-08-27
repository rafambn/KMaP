package com.rafambn.kmap.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.rememberComponentMeasurePolicy
import com.rafambn.kmap.components.rememberComponentProviderLambda
import com.rafambn.kmap.components.rememberKMaPContent
import com.rafambn.kmap.render.GraphiteMap
import com.rafambn.kmap.render.MapRenderBackend
import com.rafambn.kmap.render.graphiteIncompatibility
import com.rafambn.kmap.render.resolveGraphiteBackend

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KMaP(
    modifier: Modifier = Modifier,
    mapState: MapState,
    renderBackend: MapRenderBackend = MapRenderBackend.Auto,
    content: KMaPContent.() -> Unit,
) {
    val currentContent = rememberKMaPContent(content, mapState)
    var graphiteFailure by remember { mutableStateOf<Throwable?>(null) }

    if (renderBackend == MapRenderBackend.Graphite) {
        graphiteFailure?.let { failure ->
            throw IllegalStateException("Graphite rendering failed", failure)
        }
    }

    val incompatibility = if (renderBackend == MapRenderBackend.Compose) {
        null
    } else {
        currentContent.value.graphiteIncompatibility()
    }
    val useGraphite = resolveGraphiteBackend(
        renderBackend = renderBackend,
        contentIncompatibility = incompatibility,
        graphiteFailed = graphiteFailure != null,
    )

    val mapModifier = modifier.clipToBounds()

    if (useGraphite) {
        GraphiteMap(
            modifier = mapModifier,
            mapState = mapState,
            content = currentContent.value,
            onFatalError = { failure ->
                if (graphiteFailure == null) graphiteFailure = failure
            },
        )
    } else {
        ComposeMap(
            modifier = mapModifier.onGloballyPositioned { coordinates ->
                mapState.setCanvasSize(
                    Offset(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat(),
                    )
                )
            },
            mapState = mapState,
            content = currentContent,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposeMap(
    modifier: Modifier,
    mapState: MapState,
    content: State<KMaPContent>,
) {
    val itemProvider = rememberComponentProviderLambda(content)

    val measurePolicy = rememberComponentMeasurePolicy(
        componentProviderLambda = itemProvider,
        mapState = mapState,
    )

    LazyLayout(
        itemProvider = itemProvider,
        modifier = modifier,
        prefetchState = null,
        measurePolicy = measurePolicy,
    )
}
