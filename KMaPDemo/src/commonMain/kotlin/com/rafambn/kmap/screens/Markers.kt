package com.rafambn.kmap.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rafambn.kmap.DefaultCanvasGestureListener
import com.rafambn.kmap.KMaP
import com.rafambn.kmap.core.ClusterParameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.core.MarkerParameters
import com.rafambn.kmap.core.rememberMotionController
import com.rafambn.kmap.core.state.rememberMapState
import com.rafambn.kmap.utils.offsets.ProjectedCoordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import kmap.kmapdemo.generated.resources.teste
import kmap.kmapdemo.generated.resources.teste2
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MarkerMapRoot(
    navigateBack: () -> Unit
) {
    Box {
        val motionController = rememberMotionController()
        val mapState = rememberMapState(mapProperties = SimpleMapProperties())
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
                        ProjectedCoordinates(-45.949303, -21.424608),
                        drawPosition = DrawPosition.BOTTOM_RIGHT,
                        rotateWithMap = true,
                        tag = "zika"
                    ),
                    MarkerParameters(
                        ProjectedCoordinates(0.0, -21.424608),
                        drawPosition = DrawPosition.BOTTOM_RIGHT,
                        rotateWithMap = true,
                        tag = "zika"
                    ),
                )
            ) {
                Image(
                    painterResource(Res.drawable.teste),
                    "fd",
                    Modifier
                        .background(Color.Black)
                        .size(32.dp)
                        .clickable {
                            println("fsdfd")
                        }
                )
            }
        }
//        Image(
//            imageVector = vectorResource(Res.drawable.back_arrow),
//            contentDescription = "",
//            modifier = Modifier.clickable { navigateBack() }
//                .size(70.dp)
//        )
    }
}