@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

data class InflateOptions(
    val dictionary: UByteArray? = null
) {
    init {
        dictionary?.let {
            require(it.size <= 32768) { "dictionary must be 32kB or smaller, but was ${it.size} bytes" }
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InflateOptions

        if (!dictionary.contentEquals(other.dictionary)) return false

        return true
    }

    override fun hashCode(): Int {
        return dictionary?.contentHashCode() ?: 0
    }
}
open class DeflateOptions(
    val level: Int = 6,
    val mem: Int? = null,
    val dictionary: UByteArray? = null
) {
    init {
        require(level in 0..9) { "level must be in range 0..9, but was $level" }
        mem?.let { require(it in -1..12) { "mem must be -1 (default) or in range 0..12, but was $it" } }
        dictionary?.let {
            require(it.size <= 32768) { "dictionary must be 32kB or smaller, but was ${it.size} bytes" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DeflateOptions

        if (level != other.level) return false
        if (mem != other.mem) return false
        if (!dictionary.contentEquals(other.dictionary)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = level
        result = 31 * result + (mem ?: 0)
        result = 31 * result + (dictionary?.contentHashCode() ?: 0)
        return result
    }
}

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

data class FlateStreamData(val data: UByteArray, val final: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FlateStreamData

        if (final != other.final) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = final.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
