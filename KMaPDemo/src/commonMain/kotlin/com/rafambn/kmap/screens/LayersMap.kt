package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.components.canvas.tiled.TileSource
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.components.canvas.tiled.Tile
import com.rafambn.kmap.components.canvas.tiled.TileRenderResult
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
            canvas(
                tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = getGestureDetector(mapState.motionController)
            )
            canvas(
                alpha = sliderPosition,
                tileSource = LayerMapTileSource()::getTile
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

class LayerMapTileSource : TileSource {
    override suspend fun getTile(zoom: Int, row: Int, column: Int): TileRenderResult {
        val resourcePath = "drawable/map_overlay_${(row + column) % 2}.png"
        val bytes = Res.readBytes(resourcePath)
        val imageBitmap = bytes.decodeToImageBitmap()
        return TileRenderResult.Success(Tile(zoom, row, column, imageBitmap))
    }
}
