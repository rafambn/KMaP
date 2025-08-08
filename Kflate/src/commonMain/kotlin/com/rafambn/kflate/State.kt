@file:OptIn(ExperimentalUnsignedTypes::class)

package com.rafambn.kflate

data class InflateState(
    var literalMap: UShortArray? = null,
    var distanceMap: UShortArray? = null,
    var literalBits: Int? = null,
    var distanceBits: Int? = null,
    var finalFlag: Int? = null,
    var position: Int? = null,
    var byte: Int? = null,
    var lastCheck: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InflateState

        if (literalBits != other.literalBits) return false
        if (distanceBits != other.distanceBits) return false
        if (finalFlag != other.finalFlag) return false
        if (position != other.position) return false
        if (byte != other.byte) return false
        if (lastCheck != other.lastCheck) return false
        if (!literalMap.contentEquals(other.literalMap)) return false
        if (!distanceMap.contentEquals(other.distanceMap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = literalBits ?: 0
        result = 31 * result + (distanceBits ?: 0)
        result = 31 * result + (finalFlag ?: 0)
        result = 31 * result + (position ?: 0)
        result = 31 * result + (byte ?: 0)
        result = 31 * result + lastCheck
        result = 31 * result + (literalMap?.contentHashCode() ?: 0)
        result = 31 * result + (distanceMap?.contentHashCode() ?: 0)
        return result
    }
}

data class DeflateState(
    var head: UShortArray? = null,
    var prev: UShortArray? = null,
    var index: Int = 0,
    var endIndex: Int = 0,
    var waitIndex: Int = 0,
    var remainderByteInfo: Int = 0,
    var isLastChunk: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DeflateState

        if (index != other.index) return false
        if (endIndex != other.endIndex) return false
        if (waitIndex != other.waitIndex) return false
        if (remainderByteInfo != other.remainderByteInfo) return false
        if (isLastChunk != other.isLastChunk) return false
        if (!head.contentEquals(other.head)) return false
        if (!prev.contentEquals(other.prev)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + endIndex
        result = 31 * result + waitIndex
        result = 31 * result + remainderByteInfo
        result = 31 * result + isLastChunk
        result = 31 * result + (head?.contentHashCode() ?: 0)
        result = 31 * result + (prev?.contentHashCode() ?: 0)
        return result
    }
}
