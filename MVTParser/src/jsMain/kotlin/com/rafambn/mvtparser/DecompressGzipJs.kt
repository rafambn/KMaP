package com.rafambn.mvtparser

@JsModule("pako")
@JsNonModule
external object Pako {
    fun inflate(data: ByteArray): ByteArray
    fun inflateRaw(data: ByteArray): ByteArray
    fun ungzip(data: ByteArray): ByteArray
}

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }

    return try {
        Pako.ungzip(compressedBytes)
    } catch (e: Throwable) {
        console.error("Error decompressing GZIP data: ${e.message}")
        compressedBytes
    }
}
