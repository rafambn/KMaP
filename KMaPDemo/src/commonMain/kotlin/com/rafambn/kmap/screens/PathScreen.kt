package com.rafambn.kmap.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.components.PathParameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.PathGestureWrapper
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
    val path1 = PathData {
        moveTo(0F, 0F)
        lineTo(180F, -80F)
        lineTo(90F, -80F)
        lineTo(90F, 0F)
    }.toPath()
    val path2 = PathData {
        moveTo(-180F, 80F)
        lineTo(180F, 80F)
        lineTo(-180F, -80F)
        lineTo(-180F, 80F)
    }.toPath()
    var markerCoordinates by remember { mutableStateOf(Coordinates(0f, 0f)) }
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            canvas(
                parameters = CanvasParameters(id = 1, getTile = SimpleMapTileSource()::getTile),
                gestureWrapper = getGestureDetector(mapState.motionController)
            )
            path(
                parameters = PathParameters(
                    path = path1,
                    color = Color.Red,
                    style = Stroke(
                        width = 4F,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ),
                ),
                gestureWrapper = PathGestureWrapper(
                    onTap = {
                        markerCoordinates = with(mapState) {
                            it.toTilePoint().toCoordinates()
                        }
                    },
                )
            )
            path(
                parameters = PathParameters(
                    path = path2,
                    color = Color.Blue,
                    style = Fill,
                    zoomVisibilityRange = 0F..1F,
                    checkForClickInsidePath = true,
                ),
                gestureWrapper = PathGestureWrapper(
                    onTap = {
                        println("Path 2 clicked")
                        markerCoordinates = with(mapState) {
                            it.toTilePoint().toCoordinates()
                        }
                    },
                    onLongPress = {},
                    onDoubleTap = {},
                )
            )
            marker(
                marker = MarkerParameters(
                    coordinates = markerCoordinates,
                    drawPosition = DrawPosition.CENTER,
                )
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    val circleRadius =(minOf(size.width, size.height) / 2f)
                    val center = Offset(x = size.width / 2f, y = size.height / 2f)
                    drawCircle(
                        color = Color.Black,
                        radius = circleRadius,
                        center = center
                    )
                }
            }
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}
