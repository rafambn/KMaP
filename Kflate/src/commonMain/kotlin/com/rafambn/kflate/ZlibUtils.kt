@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

import com.rafambn.kflate.checksum.Adler32Checksum
import com.rafambn.kflate.error.FlateErrorCode
import com.rafambn.kflate.error.createFlateError
import com.rafambn.kflate.options.DeflateOptions

internal fun writeZlibHeader(output: UByteArray, options: DeflateOptions) {
    val level = options.level
    val compressionLevelFlag = when {
        level == 0 -> 0
        level < 6 -> 1
        level == 9 -> 3
        else -> 2
    }
    output[0] = 120u
    var headerByte1 = (compressionLevelFlag shl 6) or (if (options.dictionary != null) 32 else 0)
    headerByte1 = headerByte1 or (31 - ((output[0].toInt() shl 8) or headerByte1) % 31)
    output[1] = headerByte1.toUByte()

    options.dictionary?.let {
        val checksum = Adler32Checksum()
        checksum.update(it)
        writeBytes(output, 2, checksum.getChecksum().toLong())
    }
}

internal fun writeZlibStart(data: UByteArray, hasDictionary: Boolean): Int {
    val cmf = data[0].toInt()
    val flg = data[1].toInt()
    if ((cmf and 15) != 8 || (cmf ushr 4) > 7 || ((cmf shl 8 or flg) % 31 != 0))
        createFlateError(FlateErrorCode.INVALID_HEADER)
    val needsDictionary = (flg and 32) != 0
    if (needsDictionary != hasDictionary)
        createFlateError(FlateErrorCode.INVALID_HEADER)
    return (if ((flg ushr 3 and 4) != 0) 4 else 0) + 2
}
