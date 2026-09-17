# AGENTS.md

KMaP is a Compose Multiplatform map library.

## Build and verification

- Use `rtk` for shell commands and `rtk proxy` for the Kotlin wrapper.
- Build the library on JVM first: `rtk proxy ./kotlin build -m KMaP -p jvm`.
- Run checks with `rtk proxy ./kotlin check`. For platform-dependent gestures, also verify Android/iOS.
- Package the experimental JS browser demo with `rtk proxy ./DemoApp/jsApp/package-js-browser.sh`; the toolchain does not bundle its Skiko runtime and Compose resources automatically.
- If caches are unwritable, set `KOTLIN_SHARED_CACHE_DIR` and `KOTLIN_CLI_BOOTSTRAP_CACHE_DIR` to writable directories outside the repository. JS/Wasm package installation also needs a writable `XDG_DATA_HOME`.

## Project rules

- Keep common code in `src/` and platform HTTP clients in `src@<platform>/`.
- Use coordinate reference types from `ReferenceUtils.kt` across layers. Preserve zoom, rotation, density, and map border behavior when changing conversions in `MapState`.
- Canvas IDs must be unique.
- When adding overlays, update `Component`, parameters, measure/provider logic, and DSL examples together.
- Tile sources return `TileResult`.
- Vector work is paused pending Compose support for async measurement/drawing. Verify vector changes carefully.
- Keep `iosX64` disabled; current Compose artifacts do not support the Intel iOS simulator.
- Before publishing, update `settings.publishing.version` in `KMaP/module.yaml`.

## References

- Website: https://kmap.rafambn.com/
- WASM demo: https://kmap.rafambn.com/kmapdemo/
- Repo: https://github.com/rafambn/KMaP
- Mapbox Style Spec: https://docs.mapbox.com/style-spec/
- Vector Tile Spec: https://github.com/mapbox/vector-tile-spec
