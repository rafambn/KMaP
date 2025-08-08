@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate.options

class GzipOptions(
    level: Int = 6,
    mem: Int? = null,
    dictionary: UByteArray? = null,
    val filename: String? = null,
    val mtime: Any? = null
) : DeflateOptions(level, mem, dictionary) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as GzipOptions

        if (filename != other.filename) return false
        if (mtime != other.mtime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (filename?.hashCode() ?: 0)
        result = 31 * result + (mtime?.hashCode() ?: 0)
        return result
    }
}
