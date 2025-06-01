package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.OSMTileSource
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.detectPathGestures
import com.rafambn.kmap.getGestureDetector
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun PathScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    val path = PathData {
        moveTo(0F, 0F)
        lineTo(100F, 100F)
        lineTo(200F, 200F)
        lineTo(100F, 200F)
        lineTo(100F, 100F)
    }.toPath()
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            canvas(
                parameters = CanvasParameters(id = 1, getTile = SimpleMapTileSource()::getTile),
                gestureDetection = getGestureDetector(mapState.motionController)
            )
            path(
                parameters = PathParameters(
                    path = path,
                    color = Color.Red,
                    style = Stroke(
                        width = 4F,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ),
                ),
                gestureDetection = {
                    detectPathGestures(
                        onTap = {
                            println("Tap detected at $it")
                        },
                        onDoubleTap = { coordinates ->
                            println("Double tap detected at $coordinates")
                        },
                        onLongPress = { coordinates ->
                            println("Long press detected at $coordinates")
                        },
                        mapState = mapState,
                        path = path
                    )
                }
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
