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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.DefaultCanvasGestureListener
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import kmap.kmapdemo.generated.resources.pin
import kmap.kmapdemo.generated.resources.teste
import kmap.kmapdemo.generated.resources.teste2
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.random.Random

@Composable
fun MarkerMapRoot(
    navigateBack: () -> Unit
) {
    Box {
        val motionController = rememberMotionController()
        val mapState = rememberMapState(mapProperties = SimpleMapProperties())
        val markersList = remember { mutableStateListOf<MarkerParameters>() }
        KMaP(
            modifier = Modifier.align(Alignment.Center).fillMaxSize(),
            motionController = motionController,
            mapState = mapState,
            canvasGestureListener = DefaultCanvasGestureListener()
        ) {
            canvas(tileSource = SimpleMapTileSource()::getTile)
            markers(
                listOf(
                    MarkerParameters(
                        ProjectedCoordinates(-0.0, -0.0),
                        drawPosition = DrawPosition.TOP_RIGHT,
                        scaleWithMap = false,
                        tag = "Fixed size"
                    )
                )
            ) {
                Text(
                    text = "Fixed size",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                )
            }
            markers(
                listOf(
                    MarkerParameters(
                        ProjectedCoordinates(-0.0, -10.0),
                        drawPosition = DrawPosition.TOP_RIGHT,
                        scaleWithMap = true,
                        zoomToFix = 1F,
                        tag = "Scale with zoom"
                    )
                )
            ) {
                Text(
                    text = "Scale with zoom",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                )
            }
            markers(
                listOf(
                    MarkerParameters(
                        ProjectedCoordinates(-0.0, 20.0),
                        drawPosition = DrawPosition.TOP_RIGHT,
                        scaleWithMap = false,
                        rotateWithMap = true,
                        rotation = -45.0,
                        tag = "Rotate with map and custom start angle"
                    )
                )
            ) {
                Text(
                    text = "Rotate with map",
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(16.dp)
                )
            }
            markers(
                listOf(
                    MarkerParameters(
                        ProjectedCoordinates(-90.0, 0.0),
                        drawPosition = DrawPosition.TOP_RIGHT,
                        scaleWithMap = false,
                        tag = "Clickable"
                    )
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
                        .padding(16.dp)
                )
            }
            markers(markersList) {//TODO fix/improve this
                Image(
                    painter = painterResource(Res.drawable.pin),
                    contentDescription = "Removable marker",
                    modifier = Modifier
                        .clickable { markersList.remove(it) }
                        .size(32.dp)
                )
            }
        }
        Button(
            onClick = {
                markersList.add(
                    MarkerParameters(
                        ProjectedCoordinates(90.0, 45.0),
                        drawPosition = DrawPosition.TOP_RIGHT,
                        scaleWithMap = false,
                        tag = "Removable marker"
                    )
                )
                println(markersList.size)
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