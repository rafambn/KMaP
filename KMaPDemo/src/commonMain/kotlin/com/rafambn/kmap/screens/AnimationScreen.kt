package com.rafambn.kmap.screens

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.RasterCanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.vectorResource

@Composable
fun AnimationScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    var description by remember { mutableStateOf("No Animation") }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    fun startAnimation() {
        job?.cancel()

        job = scope.launch {
            mapState.motionController.animate {
                withContext(Dispatchers.Main) { description = "Panning" }
                positionTo(Coordinates(0.0, 0.0), TweenSpec(2000))
                positionTo(Coordinates(180.0, 90.0), TweenSpec(2000))
                positionTo(Coordinates(45.0, 0.0), TweenSpec(2000))
                withContext(Dispatchers.Main) { description = "Zooming by 1 level" }
                zoomBy(1F, TweenSpec(2000))
                zoomBy(-1F, TweenSpec(2000))
                withContext(Dispatchers.Main) { description = "Zooming centered on (0.0, 0.0)" }
                zoomToCentered(1F, Coordinates(0.0, 0.0), TweenSpec(2000))
                withContext(Dispatchers.Main) { description = "Rotating around screen center" }
                rotateBy(360.0, TweenSpec(2000))
                withContext(Dispatchers.Main) { description = "Rotating centered on (0.0, 0.0)" }
            }
            mapState.motionController.animate {
                rotateByCentered(-360.0, Coordinates(0.0, 0.0), TweenSpec(2000))
                withContext(Dispatchers.Main) { description = "No Animation" }
            }
        }
    }

    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            rasterCanvas(
                parameters = RasterCanvasParameters(id = 1, tileSource = SimpleMapTileSource()::getTile),
                gestureWrapper = getGestureDetector(mapState.motionController)
            )
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
        Button(
            onClick = {
                startAnimation()
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Text("Click to animate")
        }
        Text(
            text = description,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black),
            color = Color.Red
        )
    }
}
