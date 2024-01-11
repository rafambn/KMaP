package io.github.rafambn.kmap

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skia.Image

import org.jetbrains.compose.resources.Resource

actual fun isSyncResourceLoadingSupported(): Boolean = true

@OptIn(ExperimentalResourceApi::class)
actual fun Resource.readBytesSync(): ByteArray = runBlocking { readBytes() }

actual fun ByteArray.toImageBitmap(): ImageBitmap = Image.makeFromEncoded(this).toComposeImageBitmap()