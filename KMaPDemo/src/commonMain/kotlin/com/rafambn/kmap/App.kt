package com.rafambn.kmap

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafambn.kmap.screens.LayerMapRoot
import com.rafambn.kmap.screens.MarkerMapRoot
import com.rafambn.kmap.screens.SimpleMapRoot
import com.rafambn.kmap.screens.StartRoot
import com.rafambn.kmap.theme.AppTheme


@Composable
fun App() = AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navigationController = rememberNavController()
        NavHost(
            navController = navigationController,
            startDestination = Routes.Start,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Routes.Start> {
                StartRoot(
                    navigateSimpleMap = { navigationController.navigate(Routes.SimpleMap) },
                    navigateLayers = { navigationController.navigate(Routes.LayerMap) },
                    navigateMarkers = { navigationController.navigate(Routes.MarkersMap) },
                    navigatePath = {},
                    navigateAnimation = {},
                    navigateOSM = {},
                    navigateClustering = {},
                    navigateWidgets = {}
                )
            }
            composable<Routes.SimpleMap> {
                SimpleMapRoot(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.LayerMap> {
                LayerMapRoot(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
            composable<Routes.MarkersMap> {
                MarkerMapRoot(
                    navigateBack = { navigationController.popBackStack() }
                )
            }
        }
    }
}