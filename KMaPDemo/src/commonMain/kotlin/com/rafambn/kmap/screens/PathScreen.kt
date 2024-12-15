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
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun PathScreen(
    navigateBack: () -> Unit
) {
    val motionController = rememberMotionController()
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            motionController = motionController,
            mapState = mapState,
        ) {
            canvas(
                tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = getGestureDetector(motionController)
            )
            path(
                PathParameters(
                    origin = Coordinates(130.0, 0.0),
                    path = PathData {
                        moveTo(0F, 0F)
                        lineTo(100F, 100F)
                        lineTo(200F, 200F)
                        lineTo(100F, 200F)
                        lineTo(100F, 100F)
                    }.toPath(),
                    color = Color.Red,
                    style = Stroke(
                        width = 0.04F,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
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