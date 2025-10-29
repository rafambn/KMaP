package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.mapSource.tiled.RasterTile
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileResult
import com.rafambn.kmap.mapSource.tiled.raster.RasterTileSource
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.vectorResource

@Composable
fun LayersScreen(
    navigateBack: () -> Unit
) {
    Box {
        val mapState = rememberMapState(mapProperties = SimpleMapProperties())
        var sliderPosition by remember { mutableStateOf(0f) }

        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            rasterCanvas(
                parameters = RasterCanvasParameters(id = 1, tileSource = SimpleMapTileSource()::getTile),
                gestureWrapper = getGestureDetector(mapState.motionController)
            )
            rasterCanvas(
                parameters = RasterCanvasParameters(id = 2, tileSource = LayerMapTileSource()::getTile, alpha = sliderPosition),
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..1f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}

class LayerMapTileSource : RasterTileSource {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): RasterTileResult {
        val resourcePath = "drawable/map_overlay_${(row + column) % 2}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return RasterTileResult.Success(RasterTile(zoom, row, column, imageBitmap))
    }
}
