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
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.components.parameters.VectorCanvasParameters
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.mapProperties.TileDimension
import com.rafambn.kmap.mapProperties.ZoomLevelRange
import com.rafambn.kmap.mapProperties.border.BoundaryMode
import com.rafambn.kmap.mapProperties.border.MapBoundaryBehavior
import com.rafambn.kmap.mapProperties.border.TileRepeatMode
import com.rafambn.kmap.rememberMapState
import com.rafambn.kmap.source.OSMMapProperties
import com.rafambn.kmap.source.VectorTileSource
import com.rafambn.kmap.style.StyleResolver
import com.rafambn.kmap.style.compiled.CompiledStyle
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun VectorTileScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            boundaryBehavior = MapBoundaryBehavior(horizontal = BoundaryMode.CLAMP, vertical = BoundaryMode.CLAMP),
            tileRepeatMode = TileRepeatMode.NONE,
            zoomLevels = ZoomLevelRange(min = 0, max = 14),
            tileSize = TileDimension(512.dp, 512.dp)
        )
    )
    val styleState = remember { mutableStateOf<CompiledStyle?>(null) }
    val fonts = demoStyleFonts()

    LaunchedEffect(fonts) {
        val styleJson = Res.readBytes("files/map-tiler-streets.json").decodeToString()
        val sprites = loadDemoSprites(styleJson)
        styleState.value = StyleResolver().resolve(styleJson, sprites = sprites, glyphs = fonts, locale = "pt").style
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
