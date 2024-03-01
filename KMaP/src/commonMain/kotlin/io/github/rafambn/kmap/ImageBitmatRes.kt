package io.github.rafambn.kmap

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

@OptIn(InternalResourceApi::class)
inline fun imageBitmapResource(
    res: String,
    crossinline imageBitmap: (ImageBitmap) -> Unit
) {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    coroutineScope.launch {
        imageBitmap.invoke(readResourceBytes(res).toImageBitmap())
    }
}

expect fun ByteArray.toImageBitmap(): ImageBitmap