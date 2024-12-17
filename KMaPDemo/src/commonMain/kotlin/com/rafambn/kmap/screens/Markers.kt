package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import kmap.kmapdemo.generated.resources.pin
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.random.Random

@Composable
fun MarkersScreen(
    navigateBack: () -> Unit
) {
    Box {
        val motionController = rememberMotionController()
        val mapState = rememberMapState(mapProperties = SimpleMapProperties())
        val markersList = remember { mutableStateListOf<MarkerParameters>() }
        var draggableMarkerPos by remember { mutableStateOf(Coordinates(-90.0, 20.0)) }
        KMaP(
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
            motionController = motionController,
            mapState = mapState,
        ) {
            canvas(
                tileSource = SimpleMapTileSource()::getTile,
                gestureDetection = getGestureDetector(motionController)
            )
            marker(
                MarkerParameters(
                    Coordinates(0.0, 0.0),
                    drawPosition = DrawPosition.TOP_RIGHT,
                )
            ) {
                Text(
                    text = "Fixed size",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(0.0, 10.0),
                    drawPosition = DrawPosition.TOP_RIGHT,
                    zoomToFix = 1F,
                )
            ) {
                Text(
                    text = "Scale with zoom",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(0.0, -20.0),
                    drawPosition = DrawPosition.TOP_RIGHT,
                    rotateWithMap = true,
                    rotation = -45.0,
                )
            ) {
                Text(
                    text = "Rotate with map",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(90.0, 0.0),
                    drawPosition = DrawPosition.TOP_RIGHT,
                )
            ) {
                val cor = remember { mutableStateOf(Color.Black) }
                val count = remember { mutableStateOf(0) }
                val rnd = remember { Random(545) }
                Text(
                    text = "Clicked ${count.value} times",
                    modifier = Modifier
                        .clickable {
                            cor.value = Color(
                                rnd.nextInt(256),
                                rnd.nextInt(256),
                                rnd.nextInt(256),
                                255
                            )
                            count.value++
                        }
                        .background(cor.value)
                        .padding(16.dp),
                    color = Color.Red
                )
            }
            markers(markersList) {
                Image(
                    painter = painterResource(Res.drawable.pin),
                    contentDescription = "Removable marker",
                    modifier = Modifier
                        .clickable { markersList.remove(it) }
                        .size(32.dp)
                )
            }
            marker(
                MarkerParameters(
                    draggableMarkerPos,
                    drawPosition = DrawPosition.TOP_RIGHT,
                )
            ) {
                Text(
                    text = "Draggable marker",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                with(mapState) {
                                    change.consume()
                                    draggableMarkerPos =
                                        (draggableMarkerPos.toTilePoint() - dragAmount.asDifferentialScreenOffset().toTilePoint()).toCoordinates()
                                }
                            }
                        },
                    color = Color.White
                )
            }
        }
        Button(
            onClick = {
                markersList.add(
                    MarkerParameters(
                        Coordinates(-90.0, 0.0),
                        drawPosition = DrawPosition.BOTTOM_CENTER,
                    )
                )
            },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text(text = "Click to add marker")
        }
        Image(
            imageVector = vectorResource(Res.drawable.back_arrow),
            contentDescription = "",
            modifier = Modifier.clickable { navigateBack() }
                .size(70.dp)
        )
    }
}