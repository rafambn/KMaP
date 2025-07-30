package com.rafambn.gziputils

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.get
import org.khronos.webgl.set

@JsModule("pako")
@JsNonModule
external object Pako {
    fun ungzip(data: Uint8Array): Uint8Array
    fun gzip(data: Uint8Array): Uint8Array
}

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }

    val uint8Array = compressedBytes.toUint8Array()
    val decompressedUint8Array = Pako.ungzip(uint8Array)
    return decompressedUint8Array.toByteArray()
}

actual fun compressGzip(data: ByteArray): ByteArray {
    if (data.isEmpty()) return data

    val uint8Array = data.toUint8Array()
    val compressedUint8Array = Pako.gzip(uint8Array)
    return compressedUint8Array.toByteArray()
}

private fun ByteArray.toUint8Array(): Uint8Array {
    val uint8Array = Uint8Array(ArrayBuffer(size))
    for (i in indices) {
        uint8Array[i] = this[i]
    }
    return uint8Array
}

private fun Uint8Array.toByteArray(): ByteArray {
    val byteArray = ByteArray(length)
    for (i in 0 until length) {
        byteArray[i] = this[i]
    }
    return byteArray
}

