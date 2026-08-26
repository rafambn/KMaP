package com.rafambn.kmap.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.core.MapState

@Composable
internal expect fun GraphiteMap(
    modifier: Modifier,
    mapState: MapState,
    content: KMaPContent,
    onFatalError: (Throwable) -> Unit,
)

internal expect fun platformGraphiteIncompatibility(): String?
