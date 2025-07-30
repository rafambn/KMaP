package com.rafambn.gziputils

@JsModule("pako")
@JsName("pako")
external object Pako {//TODO fix issues with wasm
    @OptIn(ExperimentalUnsignedTypes::class)
    fun gzip(data: JsArray<JsNumber>): JsArray<JsNumber>
    @OptIn(ExperimentalUnsignedTypes::class)
    fun inflate(data: JsArray<JsNumber>): JsArray<JsNumber>
}

actual fun decompressGzip(compressedBytes: ByteArray): ByteArray {
    if (compressedBytes.isEmpty()) return compressedBytes

    if (compressedBytes.size < 2 || compressedBytes[0].toInt() and 0xFF != 0x1F || compressedBytes[1].toInt() and 0xFF != 0x8B) {
        return compressedBytes
    }
    val converted = compressedBytes.toTypedArray().map { it.toInt().toJsNumber() }.toJsArray()
    return Pako.inflate(converted).toArray().map { it.toInt().toByte() }.toByteArray()
}

actual fun compressGzip(data: ByteArray): ByteArray {
    if (data.isEmpty()) return data
    val converted = data.toTypedArray().map { it.toInt().toJsNumber() }.toJsArray()
    return Pako.gzip(converted).toArray().map { it.toInt().toByte() }.toByteArray()
}
