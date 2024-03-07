package io.github.rafambn.kmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


fun Float.degreesToRadian(): Float {
    return (this * PI / 180).toFloat()
}

fun Offset.rotateVector(angleRadians: Float): Offset {
    return Offset(
        (this.x * cos(angleRadians) - this.y * sin(angleRadians)), (this.x * sin(angleRadians) + this.y * cos(angleRadians))
    )
}


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