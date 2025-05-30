package com.rafambn.kmap.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rafambn.kmap.core.MapState

@Composable
fun rememberComponentProviderLambda(content: KMaPScope.() -> Unit, mapState: MapState): () -> KMaPContent {
    val latestContent = rememberUpdatedState(content)

    return remember(mapState) {
        val kMaPContentState = derivedStateOf(referentialEqualityPolicy()) {
            KMaPContent(latestContent.value)
        }
        kMaPContentState::value
    }
}
