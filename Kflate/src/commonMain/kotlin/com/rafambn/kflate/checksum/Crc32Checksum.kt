@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate.checksum


internal val CRC32_TABLE = IntArray(256).apply {
    for (i in 0 until 256) {
        var c = i
        repeat(8) {
            val mask = if ((c and 1) != 0) -306674912 else 0
            c = mask xor (c ushr 1)
        }
        this[i] = c
    }
}

internal class Crc32Checksum : ChecksumGenerator {
    private var crc = -1

    override fun update(data: UByteArray) {
        for (byte in data) {
            crc = CRC32_TABLE[(crc and 0xFF) xor byte.toInt()] xor (crc ushr 8)
        }
    }

    override fun getChecksum(): Int {
        return crc.inv()
    }
}

