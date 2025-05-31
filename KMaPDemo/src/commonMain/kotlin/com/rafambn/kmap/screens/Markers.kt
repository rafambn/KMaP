package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import com.rafambn.kmap.utils.asDifferentialScreenOffset
import com.rafambn.kmap.utils.minus
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
        val mapState = rememberMapState(mapProperties = SimpleMapProperties())
        val markersList = remember { mutableStateListOf<MarkerParameters>() }
        var draggableMarkerPos by remember { mutableStateOf(Coordinates(-90.0, 20.0)) }
        KMaP(
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
            mapState = mapState,
        ) {
            canvas(
                parameters = CanvasParameters(getTile = SimpleMapTileSource()::getTile),
                gestureDetection = getGestureDetector(mapState.motionController)
            )
            marker(
                marker = MarkerParameters(
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
                marker = MarkerParameters(
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
                marker = MarkerParameters(
                    Coordinates(0.0, 30.0),
                    drawPosition = DrawPosition.TOP_RIGHT,
                    zoomVisibilityRange = 0F..0.5F,
                )
            ) {
                Text(
                    text = "Visible range 0 to 0.5",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                marker = MarkerParameters(
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
                marker = MarkerParameters(
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
            markers(markers = markersList) { item, index ->
                Image(
                    painter = painterResource(Res.drawable.pin),
                    contentDescription = "Removable marker",
                    modifier = Modifier
                        .clickable { markersList.remove(item) }
                        .size(32.dp)
                )
            }
            marker(
                marker = MarkerParameters(
                    draggableMarkerPos,
                    drawPosition = DrawPosition.TOP_RIGHT,
                )
            ) {
                Text(
                    text = "Draggable marker Coordinates(${it.coordinates.x.toInt()}, ${it.coordinates.y.toInt()})",
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                with(mapState) {
                                    change.consume()
                                    draggableMarkerPos =
                                        (draggableMarkerPos.toTilePoint() - dragAmount.asDifferentialScreenOffset().toTilePoint()).toCoordinates()
                                }
                            }
                        }
                        .background(Color.Black)
                        .padding(16.dp),
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
