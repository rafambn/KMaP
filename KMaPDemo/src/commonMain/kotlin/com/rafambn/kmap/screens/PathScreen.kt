package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.canvas
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.asScreenOffset
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
    }
}