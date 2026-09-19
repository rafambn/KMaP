package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.overlay.VectorCanvasParameters
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.rememberMapState
import com.rafambn.kmap.source.OSMMapProperties
import com.rafambn.kmap.source.SimpleZoomLevelRange
import com.rafambn.kmap.source.VectorTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.map.TileDimension
import com.rafambn.kmap.map.border.BoundMapBorder
import com.rafambn.kmap.map.border.MapBorderType
import com.rafambn.kmap.map.border.OutsideTilesType
import com.rafambn.kmap.style.OptimizedStyle
import com.rafambn.kmap.style.Style
import com.rafambn.kmap.style.StyleResolver
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.vectorResource

@OptIn(InternalResourceApi::class)
@Composable
fun VectorTileScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            boundMap = BoundMapBorder(horizontal = MapBorderType.BOUND, vertical = MapBorderType.BOUND),
            outsideTiles = OutsideTilesType.NONE,
            zoomLevels = SimpleZoomLevelRange(max = 14),
            tileSize = TileDimension(512.dp, 512.dp)
        )
    )
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = false
    }
    val styleState = remember { mutableStateOf<OptimizedStyle?>(null) }

    LaunchedEffect(Unit) {
        val styleJson = Res.readBytes("files/stylev4.json").decodeToString()
        styleState.value = StyleResolver().resolve(json.decodeFromString<Style>(styleJson), locale = "pt")
    }

    styleState.value?.let { style ->
        Box {
            KMaP(
                modifier = Modifier.fillMaxSize(),
                mapState = mapState,
            ) {
                vectorCanvas(
                    parameters = VectorCanvasParameters(id = 1, tileSource = VectorTileSource()::getTile, style = style),
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
}
