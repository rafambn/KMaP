package com.rafambn.gziputils

@JsModule("pako")
@JsNonModule
external object Pako {
    fun ungzip(data: ByteArray): ByteArray
    fun gzip(data: ByteArray): ByteArray
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

actual fun compressGzip(data: ByteArray): ByteArray {
    if (data.isEmpty()) return data

    return try {
        Pako.gzip(data)
    } catch (e: Throwable) {
        console.error("Error compressing GZIP data: ${e.message}")
        data
    }
}
