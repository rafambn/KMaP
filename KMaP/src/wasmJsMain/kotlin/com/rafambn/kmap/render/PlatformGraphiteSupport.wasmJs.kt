@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.rafambn.kmap.render

internal actual fun platformGraphiteIncompatibility(): String? = when {
    !isBrowser() -> "Graphite is unavailable outside a browser"
    !hasWebGpu() -> "Graphite requires WebGPU"
    else -> null
}

@JsFun("() => typeof window !== 'undefined'")
private external fun isBrowser(): Boolean

@JsFun("() => typeof navigator !== 'undefined' && navigator.gpu != null")
private external fun hasWebGpu(): Boolean
