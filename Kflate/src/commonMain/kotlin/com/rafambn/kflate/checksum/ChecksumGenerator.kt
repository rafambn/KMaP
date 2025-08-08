package com.rafambn.kflate.checksum

@OptIn(ExperimentalUnsignedTypes::class)
internal interface ChecksumGenerator {
    fun update(data: UByteArray)
    fun getChecksum(): Int
}
