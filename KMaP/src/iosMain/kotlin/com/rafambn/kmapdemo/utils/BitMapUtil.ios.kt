package com.rafambn.kmapdemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap = Image.makeFromEncoded(this).toComposeImageBitmap()