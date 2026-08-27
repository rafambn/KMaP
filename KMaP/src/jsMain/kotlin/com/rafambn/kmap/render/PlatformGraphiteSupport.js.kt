package com.rafambn.kmap.render

internal actual fun platformGraphiteIncompatibility(): String? = when {
    !isBrowser() -> "Graphite is unavailable outside a browser"
    !hasWebGpu() -> "Graphite requires WebGPU"
    else -> null
}

private fun isBrowser(): Boolean = js("typeof window !== 'undefined'")

private fun hasWebGpu(): Boolean =
    js("typeof navigator !== 'undefined' && navigator.gpu != null")
