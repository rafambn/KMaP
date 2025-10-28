package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.mapProperties.border.BoundMapBorder
import com.rafambn.kmap.mapProperties.border.MapBorderType
import com.rafambn.kmap.mapProperties.border.OutsideTilesType
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.OSMMapProperties
import com.rafambn.kmap.customSources.OSMTileSource
import com.rafambn.kmap.getGestureDetector
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun OSMRemoteScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            boundMap = BoundMapBorder(horizontal = MapBorderType.BOUND, vertical = MapBorderType.BOUND),
            outsideTiles = OutsideTilesType.NONE
        )
    )
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            rasterCanvas(
                parameters = RasterCanvasParameters(id = 1, tileSource = OSMTileSource("com.rafambn.kmapdemoapp")::getTile),
                gestureWrapper = getGestureDetector(mapState.motionController)
            )
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}
