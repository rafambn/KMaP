# AGENTS.md

Compact guide for agents working in KMaP.

## Project

KMaP is a Compose Multiplatform map library. Published module: `com.rafambn:KMaP`; source version: `0.5.0`.

Targets: Android, JVM/Desktop, JS browser/node, WASM browser/node/d8, iOS Arm64/Simulator Arm64, macOS Arm64. Android uses `compileSdk = 36`, `minSdk = 24`, demo `targetSdk = 36`. JVM toolchain: 17.
`iosX64` is intentionally omitted because the current Compose artifacts do not support the Intel iOS simulator.

Build stack: Kotlin Toolchain wrapper `0.12.2`, Kotlin `2.4.10`, Compose `1.11.1`, JDK `17`.

Modules: `KMaP/` library and `DemoApp/` apps (`shared`, `androidApp`, `desktopApp`, `jsApp`, `webApp`). `DemoApp/iosApp/` is the native iOS wrapper. `mkdocs/` holds docs and WASM demo output.

## Commands

Use `rtk` before shell commands when available.

```bash
# Build / verify
./kotlin build
./kotlin build -m KMaP
./kotlin build -m shared
./kotlin build -m androidApp -p android
./kotlin check

# Fast local iteration
./kotlin build -m KMaP -p jvm
./kotlin run -m desktopApp
./kotlin package -m desktopApp

# Platform outputs
./kotlin build -m androidApp -p android -v debug
./scripts/package-js-browser.sh
./kotlin build -m webApp -p wasmJs -v release
# Open DemoApp/iosApp/module.xcodeproj in Xcode for the iOS app.

# Publish, needs signing + Maven Central creds
./kotlin publish mavenCentral
```

Sandbox note: if the default Kotlin cache is not writable, set `KOTLIN_SHARED_CACHE_DIR` and `KOTLIN_CLI_BOOTSTRAP_CACHE_DIR` to writable directories outside the repository.
For JS/Wasm package installation in the same environment, set `XDG_DATA_HOME` to a writable directory as well.

`jsApp` is an experimental browser distribution. The Kotlin Toolchain currently
does not package `js/app` automatically; use `scripts/package-js-browser.sh`,
which adds the Skiko runtime and Compose resources to the generated module output.

## Code Map

Main paths:

- `KMaP/src/com/rafambn/kmap/core/` - `KMaP`, `MapState`, `CameraState`, `MotionController`.
- `KMaP/src/com/rafambn/kmap/components/` - DSL and overlay types: raster/vector canvas, markers, clusters, paths.
- `KMaP/src/com/rafambn/kmap/mapSource/tiled/` - tile API, tile results, raster/vector tiles, canvas engines, renderer/cache.
- `KMaP/src/com/rafambn/kmap/mapProperties/` - bounds, tile size, zoom range, coordinate projection config.
- `KMaP/src/com/rafambn/kmap/gestures/` - map/path gesture detection and wrappers.
- `KMaP/src/com/rafambn/kmap/utils/` - reference coordinate types, math, rotation, MVT parser, Mapbox/MapLibre style support.
- `DemoApp/shared/src/com/rafambn/kmap/screens/` - feature demos.
- `DemoApp/shared/src/com/rafambn/kmap/customSources/` - sample tile/map sources.
- `DemoApp/shared/src@<platform>/` - platform-specific source sets; `composeResources/` contains shared Compose resources.

## Architecture

Flow: pointer input -> gesture wrapper -> `MotionController` -> `MapState.cameraState` -> `CanvasKernel.resolveVisibleTiles()` -> `CanvasEngine`/`TileRenderer` -> Compose canvas + overlays.

`KMaP()` is a `LazyLayout`. `KMaPContent` declares layers and overlays, then refreshes `MapState.canvasKernel` from canvas parameters. Canvas IDs must be unique.

Tile source contract:

```kotlin
interface TileSource<T : Tile> {
    suspend fun getTile(zoom: Int, row: Int, column: Int): TileResult<T>
}
```

Canvas parameters accept `tileSource` functions directly:

- `RasterCanvasParameters(..., tileSource: suspend (...) -> TileResult<RasterTile>)`
- `VectorCanvasParameters(..., tileSource: suspend (...) -> TileResult<VectorTile>, style: OptimizedStyle)`

Vector tiles exist, but project docs mark vector work paused until Compose has async measurement/drawing support. Treat raster path as production-ready; verify vector changes carefully.

## Coordinate Rules

Use reference types from `ReferenceUtils.kt`; do not pass raw `Offset`/pairs across layers unless API demands it.

- `Coordinates` - longitude/latitude.
- `ProjectedCoordinates` - projected map coordinates.
- `TilePoint` - normalized map/tile space used by camera and components.
- `ScreenOffset` - pixels from top-left of `KMaP`.
- `DifferentialScreenOffset` - pixel delta.
- `CanvasDrawReference` - draw-space origin.

Conversions live mostly on `MapState`; preserve zoom, rotation, density, and map border behavior when editing them.

## Change Rules

- Keep common code in `src/`; platform HTTP clients belong in `src@<platform>/`.
- Add new public map behavior through `MapState`, `MotionController`, or `KMaPContent` only when it fits existing layering.
- Add new overlays by updating `Component`, parameters, measure/provider logic, and DSL examples together.
- Add new tile sources under `mapSource/` or demo `customSources/`; return `TileResult`, not raw bitmaps/data.
- Test fastest target first: JVM demo/tests. For gesture changes, also check Android/iOS if behavior depends on pointer platform.
- Before publishing, update `settings.publishing.version` in `KMaP/module.yaml`; docs may still reference the previous Maven Central version.

## References

- Website: https://kmap.rafambn.com/
- WASM demo: https://kmap.rafambn.com/kmapdemo/
- Repo: https://github.com/rafambn/KMaP
- Mapbox Style Spec: https://docs.mapbox.com/style-spec/
- Vector Tile Spec: https://github.com/mapbox/vector-tile-spec
