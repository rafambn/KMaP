package io.github.rafambn.templateapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rafambn.kmap.KMaP
import io.github.rafambn.kmap.states.rememberCameraState
import io.github.rafambn.templateapp.theme.AppTheme

@Composable
internal fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box {
            val cameraState = rememberCameraState()
            KMaP(
                modifier = Modifier.align(Alignment.Center).size(300.dp, 600.dp),
                mapState = cameraState
            )
        }
    }
}