package com.rafambn.gziputils

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }

    return memScoped {
        val data = NSData.dataWithBytes(
            compressedBytes.refTo(0).getPointer(this),
            compressedBytes.size.toULong()
        )

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        val decompressedData = data.decompressedDataUsingAlgorithm(
            NSDataCompressionAlgorithmZlib,
            errorPtr.ptr
        )

        if (decompressedData == null) {
            errorPtr.value?.let { error ->
                println("Decompression error: ${error.localizedDescription}")
            }
            return@memScoped compressedBytes
        }

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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun compressGzip(data: ByteArray): ByteArray {
    if (data.isEmpty()) return data

    return memScoped {
        val nsData = NSData.dataWithBytes(
            data.refTo(0).getPointer(this),
            data.size.toULong()
        )

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        val compressedData = nsData.compressedDataUsingAlgorithm(
            NSDataCompressionAlgorithmZlib,
            errorPtr.ptr
        )

        if (compressedData == null) {
            errorPtr.value?.let { error ->
                println("Compression error: ${error.localizedDescription}")
            }
            return@memScoped data
        }

        ByteArray(compressedData.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(
                    pinned.addressOf(0),
                    compressedData.bytes,
                    compressedData.length
                )
            }
        }
    }
}
