package io.github.rafambn.kmap

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Resource
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
inline fun imageBitmapResource(
    res: String,
    isSync: Boolean = true,
    crossinline imageBitmap: (ImageBitmap) -> Unit
) {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    if (!isSyncResourceLoadingSupported()) {
        coroutineScope.launch {
            imageBitmap.invoke(resource(res).readBytes().toImageBitmap())
        }
    } else if (!isSync) {
        coroutineScope.launch {
            imageBitmap.invoke(resource(res).readBytes().toImageBitmap())
        }
    } else {
        imageBitmap.invoke(resource(res).readBytesSync().toImageBitmap())
    }
}


@OptIn(ExperimentalResourceApi::class)
expect fun Resource.readBytesSync(): ByteArray

expect fun ByteArray.toImageBitmap(): ImageBitmap

expect fun isSyncResourceLoadingSupported(): Boolean