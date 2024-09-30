package com.rafambn.kmapdemo.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap = BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()