package com.rafambn.mvtparser

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS implementation of decompressGzip using zlib
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    // Check for GZIP magic number (0x1F, 0x8B)
    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        // Not GZIP, return as is
        return compressedBytes
    }

    // Use NSData for decompression
    return memScoped {
        // Convert ByteArray to NSData
        val data = NSData.dataWithBytes(
            compressedBytes.refTo(0).getPointer(this),
            compressedBytes.size.toULong()
        )

        // Create an error pointer
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        // Use NSData's built-in decompression method
        val decompressedData = data.decompressedDataUsingAlgorithm(
            NSDataCompressionAlgorithmZlib,
            errorPtr.ptr
        )

        if (decompressedData == null) {
            // Log error if available
            errorPtr.value?.let { error ->
                println("Decompression error: ${error.localizedDescription}")
            }
            return@memScoped compressedBytes
        }

        // Convert NSData back to ByteArray
        ByteArray(decompressedData.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(
                    pinned.addressOf(0),
                    decompressedData.bytes,
                    decompressedData.length
                )
            }
        }
    }
}
