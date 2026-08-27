package com.rafambn.kmap.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.core.MapState

@Composable
internal actual fun GraphiteMap(
    modifier: Modifier,
    mapState: MapState,
    content: KMaPContent,
    onFatalError: (Throwable) -> Unit,
): Unit = error("Graphite is unavailable on macOS Arm64")

internal actual fun platformGraphiteIncompatibility(): String? =
    "Graphite is unavailable on macOS Arm64"
