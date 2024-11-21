package com.rafambn.kmap.screens

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
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.canvas
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
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
    val motionController = rememberMotionController()
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    var description by remember { mutableStateOf("No Animation") }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    fun startAnimation() {
        job?.cancel()

        job = scope.launch {
            motionController.animate {
                withContext(Dispatchers.Main) { description = "Panning" }
                positionTo(ProjectedCoordinates(0.0, 0.0))
                positionTo(ProjectedCoordinates(180.0, 90.0))
                positionTo(ProjectedCoordinates(45.0, 0.0))
                withContext(Dispatchers.Main) { description = "Zooming by 1 level" }
                zoomBy(1F)
                zoomBy(-1F)
                withContext(Dispatchers.Main) { description = "Zooming centered on (0.0, 0.0)" }
                zoomToCentered(1F, ProjectedCoordinates(0.0, 0.0))
                withContext(Dispatchers.Main) { description = "Rotating around screen center" }
                rotateBy(360.0)
                withContext(Dispatchers.Main) { description = "Rotating centered on (0.0, 0.0)" }
                rotateByCentered(-360.0, ProjectedCoordinates(0.0, 0.0))
                withContext(Dispatchers.Main) { description = "No Animation" }
            }
        }
    }

    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            motionController = motionController,
            mapState = mapState,
        ) {
            canvas(tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = {
                    detectMapGestures(
                        onTap = { offset ->
//                            canvasGestureListener.onTap(offset.asScreenOffset())
                        },
                        onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset.asScreenOffset()) } },
                        onLongPress = { offset ->
//                            canvasGestureListener.onLongPress(offset.asScreenOffset())
                        },
                        onTapLongPress = { offset -> motionController.move { positionBy(offset.asDifferentialScreenOffset()) } },
                        onTapSwipe = { centroid, zoom -> motionController.move { zoomByCentered(zoom, centroid.asScreenOffset()) } },
                        onDrag = { dragAmount -> motionController.move { positionBy(dragAmount.asDifferentialScreenOffset()) } },
                        onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset.asScreenOffset()) } },
                        onGesture = { centroid, pan, zoom, rotation ->
                            motionController.move {
                                rotateByCentered(rotation.toDouble(), centroid.asScreenOffset())
                                zoomByCentered(zoom, centroid.asScreenOffset())
                                positionBy(pan.asDifferentialScreenOffset())
                            }
                        },
                        onHover = { offset ->
//                            canvasGestureListener.onHover(offset.asScreenOffset())
                        },
                        onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount, mouseOffset.asScreenOffset()) } },
                        onCtrlGesture = { rotation -> motionController.move { rotateBy(rotation.toDouble()) } },
//                        currentGestureFlow = canvasGestureListener._currentGestureFlow
                    )
                })
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