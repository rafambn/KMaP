@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate.checksum

internal class Adler32Checksum : ChecksumGenerator {
    private var a = 1
    private var b = 0

    override fun update(data: UByteArray) {
        val len = data.size
        var i = 0
        while (i < len) {
            val end = minOf(i + 2655, len)
            while (i < end) {
                a += data[i].toInt()
                b += a
                i++
            }
            a = (a and 0xFFFF) + 15 * (a ushr 16)
            b = (b and 0xFFFF) + 15 * (b ushr 16)
        }
    }

    override fun getChecksum(): Int {
        a %= 65521
        b %= 65521
        return ((a and 0xFF) shl 24) or
                ((a and 0xFF00) shl 8) or
                ((b and 0xFF) shl 8) or
                (b ushr 8)
    }
}
