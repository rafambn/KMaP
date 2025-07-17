package com.rafambn.mvtparser

/**
 * JS implementation of decompressGzip using the pako library
 */
@JsModule("pako")
@JsNonModule
external object Pako {
    fun inflate(data: ByteArray): ByteArray
    fun inflateRaw(data: ByteArray): ByteArray
    fun ungzip(data: ByteArray): ByteArray
}

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    // Check for GZIP magic number (0x1F, 0x8B)
    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        // Not GZIP, return as is
        return compressedBytes
    }

    return try {
        // Use pako to decompress GZIP data
        Pako.ungzip(compressedBytes)
    } catch (e: Throwable) {
        console.error("Error decompressing GZIP data: ${e.message}")
        // Return original data if decompression fails
        compressedBytes
    }
}
