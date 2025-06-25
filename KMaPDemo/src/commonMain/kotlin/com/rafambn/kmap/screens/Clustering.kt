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
import com.rafambn.kmap.components.CanvasParameters
import com.rafambn.kmap.core.KMaP
import com.rafambn.kmap.components.ClusterParameters
import com.rafambn.kmap.core.DrawPosition
import com.rafambn.kmap.components.MarkerParameters
import com.rafambn.kmap.core.rememberMapState
import com.rafambn.kmap.customSources.SimpleMapProperties
import com.rafambn.kmap.customSources.SimpleMapTileSource
import com.rafambn.kmap.getGestureDetector
import com.rafambn.kmap.utils.Coordinates
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.back_arrow
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ClusteringScreen(
    navigateBack: () -> Unit
) {
    val mapState = rememberMapState(mapProperties = SimpleMapProperties())
    Box {
        KMaP(
            modifier = Modifier.fillMaxSize(),
            mapState = mapState,
        ) {
            canvas(
                parameters = CanvasParameters(id = 1, tileSource = SimpleMapTileSource()::getTile),
                gestureWrapper = getGestureDetector(mapState.motionController)
            )
            markers(
                listOf(
                    MarkerParameters(
                        Coordinates(0.0, 0.0),
                        drawPosition = DrawPosition.CENTER,
                        clusterId = 1
                    ),
                    MarkerParameters(
                        Coordinates(18.0, 0.0),
                        drawPosition = DrawPosition.CENTER,
                        clusterId = 1
                    )
                )
            ) { item, index ->
                Text(
                    text = "Tag 1",
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(0.0, 3.0),
                    drawPosition = DrawPosition.CENTER,
                    clusterId = 1
                )
            ) {
                Text(
                    text = "Tag 1",
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(15.0, -20.0),
                    drawPosition = DrawPosition.CENTER,
                    clusterId = 2
                )
            ) {
                Text(
                    text = "Tag 2",
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            marker(
                MarkerParameters(
                    Coordinates(0.0, -20.0),
                    drawPosition = DrawPosition.CENTER,
                    clusterId = 1
                )
            ) {
                Text(
                    text = "Tag 1",
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(16.dp),
                    color = Color.White
                )
            }
            cluster(
                ClusterParameters(id = 1)
            ) {
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
