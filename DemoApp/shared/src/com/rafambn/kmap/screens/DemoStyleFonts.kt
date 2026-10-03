package com.rafambn.kmap.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kmap.kmapdemo.generated.resources.Res
import kmap.kmapdemo.generated.resources.noto_sans
import kmap.kmapdemo.generated.resources.noto_sans_condensed_italic
import kmap.kmapdemo.generated.resources.noto_sans_devanagari
import kmap.kmapdemo.generated.resources.noto_sans_italic
import kmap.kmapdemo.generated.resources.open_sans
import kmap.kmapdemo.generated.resources.open_sans_italic
import kmap.kmapdemo.generated.resources.roboto
import kmap.kmapdemo.generated.resources.roboto_condensed
import kmap.kmapdemo.generated.resources.roboto_condensed_italic
import kmap.kmapdemo.generated.resources.roboto_italic
import kmap.kmapdemo.generated.resources.roboto_mono
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
internal fun demoStyleFonts(): Map<String, FontFamily> = mapOf(
    "Noto Sans Regular" to styleFont(Res.font.noto_sans),
    "Noto Sans Medium" to styleFont(Res.font.noto_sans, FontWeight.Medium),
    "Noto Sans Bold" to styleFont(Res.font.noto_sans, FontWeight.Bold),
    "Noto Sans Italic" to styleFont(Res.font.noto_sans_italic, style = FontStyle.Italic),
    "Noto Sans Condensed Italic" to styleFont(Res.font.noto_sans_condensed_italic, style = FontStyle.Italic),
    "Noto Sans Devanagari Regular v1" to styleFont(Res.font.noto_sans_devanagari),
    "Open Sans Light" to styleFont(Res.font.open_sans, FontWeight.Light),
    "Open Sans Regular" to styleFont(Res.font.open_sans),
    "Open Sans Semi Bold" to styleFont(Res.font.open_sans, FontWeight.SemiBold),
    "Open Sans Semibold" to styleFont(Res.font.open_sans, FontWeight.SemiBold),
    "Open Sans Bold" to styleFont(Res.font.open_sans, FontWeight.Bold),
    "Open Sans Italic" to styleFont(Res.font.open_sans_italic, style = FontStyle.Italic),
    "Open Sans Semi Bold Italic" to styleFont(Res.font.open_sans_italic, FontWeight.SemiBold, FontStyle.Italic),
    "Open Sans Bold Italic" to styleFont(Res.font.open_sans_italic, FontWeight.Bold, FontStyle.Italic),
    "Roboto Regular" to styleFont(Res.font.roboto),
    "Roboto Medium" to styleFont(Res.font.roboto, FontWeight.Medium),
    "Roboto Bold" to styleFont(Res.font.roboto, FontWeight.Bold),
    "Roboto Italic" to styleFont(Res.font.roboto_italic, style = FontStyle.Italic),
    "Roboto Condensed Medium" to styleFont(Res.font.roboto_condensed, FontWeight.Medium),
    "Roboto Condensed Italic" to styleFont(Res.font.roboto_condensed_italic, style = FontStyle.Italic),
    "Roboto Mono Medium" to styleFont(Res.font.roboto_mono, FontWeight.Medium)
)

@Composable
private fun styleFont(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): FontFamily = FontFamily(Font(resource, weight, style))
