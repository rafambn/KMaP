package io.github.rafambn.templateapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.DrawPosition
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.MarkerPlacer
import io.github.rafambn.kmap.Position
import io.github.rafambn.kmap.rememberMapState
import io.github.rafambn.kmap.utils.toPosition
import io.github.rafambn.templateapp.theme.AppTheme
import kmap_library_with_app.composeapp.generated.resources.Res
import kmap_library_with_app.composeapp.generated.resources.teste
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val mapState = rememberMapState(CoroutineScope(Dispatchers.Default))
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                mapState = mapState
            ) {
                markers(
                    listOf(
                        MarkerPlacer(
                            mapState.positionToCanvasOffset(Position(0.0, 28.447791)).toPosition(),
                            DrawPosition.CENTER,
                            0,
                            1F,
                            false
                        ),
                        MarkerPlacer(
                            mapState.positionToCanvasOffset(Position(0.0, 0.0)).toPosition(),
                            DrawPosition.LEFT_TOP,
                            0,
                            1F,
                            false
                        )
                    )
                ) {
                    Image(painterResource(Res.drawable.teste), "fd", Modifier.size(20F.dp))
                }
            }
        }
    }
}