package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.VectorCanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.OSMMapProperties
import com.rafambn.kmap.customSources.SimpleZoomLevelRange
import com.rafambn.kmap.customSources.VectorTileSource
import com.rafambn.kmap.customSources.remoteVectorStyle
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.render.MapRenderBackend
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun VectorTileScreen(
    navigateBack: () -> Unit,
) {
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            boundMap = BoundMapBorder(horizontal = MapBorderType.BOUND, vertical = MapBorderType.BOUND),
            outsideTiles = OutsideTilesType.NONE,
            zoomLevels = SimpleZoomLevelRange(max = 14),
            tileSize = TileDimension(512.dp, 512.dp)
        )
    )
    val tileSource = remember { VectorTileSource() }
    val style = remember { remoteVectorStyle() }

    DisposableEffect(tileSource) {
        onDispose(tileSource::close)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
            renderBackend = MapRenderBackend.Auto,
        ) {
            vectorCanvas(
                parameters = VectorCanvasParameters(
                    id = 1,
                    tileSource = tileSource::getTile,
                    style = style,
                ),
                gestureWrapper = getGestureDetector(mapState.motionController),
            )
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp),
        )
    }
}
