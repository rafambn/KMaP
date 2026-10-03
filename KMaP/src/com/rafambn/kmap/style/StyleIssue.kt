package com.rafambn.kmap.style

data class StyleIssue(
    val path: String,
    val kind: Kind,
    val message: String,
    val layerId: String? = null
) {
    enum class Kind { UNSUPPORTED, UNKNOWN, INVALID }
}
