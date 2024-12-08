package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.canvas
import com.rafambn.kmap.core.cluster
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.gestures.detectMapGestures
import com.rafambn.kmap.core.marker
import com.rafambn.kmap.core.markers
import com.rafambn.kmap.utils.ProjectedCoordinates
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource


@Composable
fun ClusteringScreen(
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
                        onDoubleTap = { offset -> motionController.move { zoomByCentered(-1 / 3F, offset) } },
                        onTapLongPress = { offset -> motionController.move { positionBy(offset.asDifferentialScreenOffset()) } },
                        onTapSwipe = { zoom -> motionController.move { zoomBy(zoom / 100) } },
                        onDrag = { dragAmount -> motionController.move { positionBy(dragAmount) } },
                        onTwoFingersTap = { offset -> motionController.move { zoomByCentered(1 / 3F, offset) } },
                        onGesture = { centroid, pan, zoom, rotation ->
                            motionController.move {
                                rotateByCentered(rotation.toDouble(), centroid)
                                zoomByCentered(zoom, centroid)
                                positionBy(pan)
                            }
                        },
                        onScroll = { mouseOffset, scrollAmount -> motionController.move { zoomByCentered(scrollAmount, mouseOffset) } },
                        onCtrlGesture = { rotation -> motionController.move { rotateBy(rotation.toDouble()) } },
                    )
                })
            markers(
                listOf(
                    MarkerParameters(
                        ProjectedCoordinates(0.0, 0.0),
                        drawPosition = DrawPosition.CENTER,
                        tag = "Tag 1"
                    ),
                    MarkerParameters(
                        ProjectedCoordinates(18.0, 0.0),
                        drawPosition = DrawPosition.CENTER,
                        tag = "Tag 1"
                    )
                )
            ) {
                Text(
                    text = it.tag,
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    ProjectedCoordinates(0.0, 3.0),
                    drawPosition = DrawPosition.CENTER,
                    tag = "Tag 1"
                )
            ) {
                Text(
                    text = it.tag,
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    ProjectedCoordinates(15.0, -20.0),
                    drawPosition = DrawPosition.CENTER,
                    tag = "Tag 2"
                )
            ) {
                Text(
                    text = it.tag,
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    ProjectedCoordinates(0.0, -20.0),
                    drawPosition = DrawPosition.CENTER,
                    tag = "Tag 1"
                )
            ) {
                Text(
                    text = it.tag,
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            cluster(
                ClusterParameters(
                    tag = "Tag 1",
                    clusterThreshold = 50.dp,
                    drawPosition = DrawPosition.CENTER,
                )
            ) { _, _ ->
                Text(
                    text = "Cluster tag 1",
                    modifier = Modifier
                        .background(Color.Green)
                        .padding(16.dp),
                    color = Color.White
                )
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