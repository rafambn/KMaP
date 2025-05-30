package com.rafambn.kmap.components.path

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import com.rafambn.kmap.components.KMaPContent
import com.rafambn.kmap.components.Parameters

@OptIn(ExperimentalFoundationApi::class)
class PathProvider(
    private val kmapContent: KMaPContent
) : LazyLayoutItemProvider {

    override val itemCount
        get() = kmapContent.paths.size

    @Composable
    override fun Item(index: Int, key: Any) {
        kmapContent.paths[index].pathContent()
    }

    fun getParameters(index: Int): Parameters = kmapContent.paths[index].parameters
}
