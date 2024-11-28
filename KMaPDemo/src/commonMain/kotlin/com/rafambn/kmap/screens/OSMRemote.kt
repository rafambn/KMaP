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
import com.rafambn.kmap.config.border.BoundMapBorder
import com.rafambn.kmap.config.border.MapBorderType
import com.rafambn.kmap.config.border.OutsideTilesType
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.customSources.OSMMapProperties
import com.rafambn.kmap.customSources.OSMTileSource
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun OSMRemoteScreen(
    navigateBack: () -> Unit
) {
    val motionController = rememberMotionController()
    val mapState = rememberMapState(
        mapProperties = OSMMapProperties(
            boundMap = BoundMapBorder(horizontal = MapBorderType.LOOP, vertical = MapBorderType.LOOP),
            outsideTiles = OutsideTilesType.LOOP
        )
    )
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            motionController = motionController,
            mapState = mapState,
        ) {
            canvas(tileSource = OSMTileSource("com.rafambn.kmapdemoapp")::getTile,
                gestureDetection = {
                    detectMapGestures(
                        onTap = { offset ->
//                            canvasGestureListener.onTap(offset.asScreenOffset())
                        },
                        onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
                        onLongPress = { offset ->
//                            canvasGestureListener.onLongPress(offset.asScreenOffset())
                        },
                        onTapLongPress = { offset -> motionController.move { positionBy(offset.asDifferentialScreenOffset()) } },
                        onTapSwipe = { zoom -> motionController.move { zoomBy(zoom) } },
                        onDrag = { dragAmount -> motionController.move { positionBy(dragAmount) } },
                        onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
                        onGesture = { centroid, pan, zoom, rotation ->
                            motionController.move {
                                rotateByCentered(rotation.toDouble(), centroid)
                                zoomByCentered(zoom, centroid)
                                positionBy(pan)
                            }
                        },
                        onHover = { offset ->
//                            canvasGestureListener.onHover(offset.asScreenOffset())
                        },
                        onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount, mouseOffset) } },
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