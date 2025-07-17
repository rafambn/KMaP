package com.rafambn.mvtparser

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }
    println("GZIP decompression not fully implemented for Wasm target due to Kotlin/Wasm JS interop limitations")
    return compressedBytes
}
