package com.rafambn.mvtparser

import kotlin.js.JsAny

external interface PakoLib : JsAny {
    fun ungzip(data: JsAny): JsAny
    fun inflate(data: JsAny): JsAny
}

private fun ByteArray.toUint8Array(): JsAny {
    val jsArray = js("new Uint8Array(arguments[0])")
    return jsArray.unsafeCast<JsAny>()
}

private fun JsAny.toByteArray(): ByteArray {
    val jsArray = this
    val size = js("arguments[0].length") as Int
    val byteArray = ByteArray(size)
    for (i in 0 until size) {
        byteArray[i] = js("arguments[0][arguments[1]]").toByte()
    }
    return byteArray
}

private fun loadPako(): PakoLib? {
    return try {
        val pako = js("globalThis.pako || window.pako")
        if (pako != null) {
            pako.unsafeCast<PakoLib>()
        } else {
            println("Pako library not found. Please include Pako.js in your HTML.")
            null
        }
    } catch (e: Throwable) {
        println("Error accessing Pako library: ${e.message}")
        null
    }
}

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }

    return try {
        val pako = loadPako()
        if (pako != null) {
            val jsData = compressedBytes.toUint8Array()
            val result = pako.ungzip(jsData)
            result.toByteArray()
        } else {
            println("Pako not available")
            compressedBytes
        }
    } catch (e: Throwable) {
        println("Error decompressing GZIP data: ${e.message}")
        compressedBytes
    }
}
