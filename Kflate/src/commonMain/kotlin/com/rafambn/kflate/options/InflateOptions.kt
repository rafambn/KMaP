@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate.options

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

