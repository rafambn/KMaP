package io.github.rafambn.kmap

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.Resource
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi

actual fun isSyncResourceLoadingSupported(): Boolean = true

@OptIn(ExperimentalResourceApi::class)
actual fun Resource.readBytesSync(): ByteArray = runBlocking { readBytes() }

actual fun ByteArray.toImageBitmap(): ImageBitmap = toAndroidBitmap().asImageBitmap()

private fun ByteArray.toAndroidBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, size);
}