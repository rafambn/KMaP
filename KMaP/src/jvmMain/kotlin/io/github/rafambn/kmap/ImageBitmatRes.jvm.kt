package io.github.rafambn.kmap

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.runBlocking

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Resource
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap = Image.makeFromEncoded(this).toComposeImageBitmap()

actual fun isSyncResourceLoadingSupported(): Boolean = true

@OptIn(ExperimentalResourceApi::class)
actual fun Resource.readBytesSync(): ByteArray = runBlocking { readBytes() }