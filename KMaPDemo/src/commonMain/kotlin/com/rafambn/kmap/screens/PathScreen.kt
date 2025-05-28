package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.detectPathGestures
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun PathScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    val lastGesture = remember { mutableStateOf("No gesture detected") }

    Box {

        // Create the path
        val pathData = PathData {
            moveTo(0F, 0F)
            lineTo(100F, 100F)
            lineTo(200F, 200F)
            lineTo(100F, 200F)
            lineTo(100F, 100F)
        }
        val path = pathData.toPath()
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            canvas(
                tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = getGestureDetector(mapState.motionController)
            )
            path(
                origin = Coordinates(0.0, 0.0),
                path = path,
                color = Color.Red,
                style = Stroke(
                    width = 4F,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                ),
                gestureDetection = {
                    detectPathGestures(
                        onTap = {
                            print("zika")
                        },
//                        onDoubleTap = { coordinates ->
//                            lastGesture.value = "Double tap detected at $coordinates"
//                            println("Path double tapped at $coordinates")
//                        },
//                        onLongPress = { coordinates ->
//                            lastGesture.value = "Long press detected at $coordinates"
//                            println("Path long pressed at $coordinates")
//                        },
                        mapState = mapState,
                        path = path,
                        threshold = 20f
                    )
                }
            )
        }

        // Display the last gesture
        Text(
            text = lastGesture.value,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            color = Color.Black
        )
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}
