# Demo map fonts

The fonts in `../../composeResources/font` come from the [Google Fonts repository](https://github.com/google/fonts) and the [Noto Sans release](https://github.com/notofonts/latin-greek-cyrillic/releases/tag/NotoSans-v2.015). They use the SIL Open Font License 1.1. The license text for each family is kept in this directory.

| Font files | Upstream family | License file |
| --- | --- | --- |
| `noto_sans.ttf`, `noto_sans_italic.ttf` | [Noto Sans](https://github.com/google/fonts/tree/main/ofl/notosans) | `OFL-NotoSans.txt` |
| `noto_sans_condensed_italic.ttf` | [Noto Sans v2.015 release](https://github.com/notofonts/latin-greek-cyrillic/releases/tag/NotoSans-v2.015) | `OFL-NotoSans.txt` |
| `noto_sans_devanagari.ttf` | [Noto Sans Devanagari](https://github.com/google/fonts/tree/main/ofl/notosansdevanagari) | `OFL-NotoSansDevanagari.txt` |
| `open_sans.ttf`, `open_sans_italic.ttf` | [Open Sans](https://github.com/google/fonts/tree/main/ofl/opensans) | `OFL-OpenSans.txt` |
| `roboto.ttf`, `roboto_italic.ttf` | [Roboto](https://github.com/google/fonts/tree/main/ofl/roboto) | `OFL-Roboto.txt` |
| `roboto_condensed.ttf`, `roboto_condensed_italic.ttf` | [Roboto Condensed](https://github.com/google/fonts/tree/main/ofl/robotocondensed) | `OFL-RobotoCondensed.txt` |
| `roboto_mono.ttf` | [Roboto Mono](https://github.com/google/fonts/tree/main/ofl/robotomono) | `OFL-RobotoMono.txt` |

The demo maps all 21 `text-font` names in the bundled map styles to these files in `DemoStyleFonts.kt`. The current Noto Sans Devanagari file may differ from the provider's font stack named `Noto Sans Devanagari Regular v1`.
