package com.rafambn.kmap.style

import com.rafambn.kmap.style.compiled.CompiledStyle
/** A null style means the input could not be decoded; issues remain available in that case. */
data class StyleResolution(
    val style: CompiledStyle?,
    val issues: List<StyleIssue>
)
