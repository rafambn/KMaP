# AGENTS.md

Compact guide for agents working in KMaP.

## Project

KMaP is a Compose Multiplatform map library. Published module: `com.rafambn:KMaP`; source version: `0.4.2`.

Targets: Android, JVM/Desktop, JS browser/node, WASM browser/node/d8, iOS, macOS Arm64. Android uses `compileSdk = 36`, `minSdk = 24`, demo `targetSdk = 36`. JVM toolchain: 17.

Build stack: Gradle wrapper `9.5.0`, Kotlin `2.3.21`, Compose `1.10.3`, AGP `9.2.0`, vanniktech maven-publish `0.36.0`.

Modules: `:KMaP` library and `DemoApp/` apps (`:DemoApp:shared`, `:DemoApp:androidApp`, `:DemoApp:desktopApp`, `:DemoApp:webApp`). `DemoApp/iosApp/` is native iOS wrapper. `mkdocs/` holds docs and WASM demo output.

## Commands

Use `rtk` before shell commands when available.

```bash
# Build / verify
rtk ./gradlew :build
rtk ./gradlew :KMaP:build
rtk ./gradlew :DemoApp:shared:build
rtk ./gradlew :DemoApp:androidApp:build
rtk ./gradlew :check

# Fast local iteration
rtk ./gradlew :KMaP:build -x test
rtk ./gradlew :DemoApp:desktopApp:run
rtk ./gradlew :DemoApp:desktopApp:run --continuous

# Platform outputs
rtk ./gradlew :DemoApp:androidApp:assembleDebug
rtk ./gradlew :KMaP:jsBrowserProductionWebpack
rtk ./gradlew :DemoApp:webApp:wasmJsBrowserDistribution
rtk ./gradlew :KMaP:linkReleaseFrameworkIosArm64

# Publish, needs signing + Maven Central creds
rtk ./gradlew :publishToMavenCentral
```

Sandbox note: if Gradle cannot write under `~/.gradle`, use writable `GRADLE_USER_HOME` outside repo only when allowed.

## Code Map

Main paths:

- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/` - `KMaP`, `MapState`, `CameraState`, `MotionController`.
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/` - DSL and overlay types: raster/vector canvas, markers, clusters, paths.
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/mapSource/tiled/` - tile API, tile results, raster/vector tiles, canvas engines, renderer/cache.
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/mapProperties/` - bounds, tile size, zoom range, coordinate projection config.
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/gestures/` - map/path gesture detection and wrappers.
- `KMaP/src/commonMain/kotlin/com/rafambn/kmap/utils/` - reference coordinate types, math, rotation, MVT parser, Mapbox/MapLibre style support.
- `DemoApp/shared/src/commonMain/kotlin/com/rafambn/kmap/screens/` - feature demos.
- `DemoApp/shared/src/commonMain/kotlin/com/rafambn/kmap/customSources/` - sample tile/map sources.

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

- Keep common code in `commonMain`; platform HTTP clients belong in target source sets.
- Add new public map behavior through `MapState`, `MotionController`, or `KMaPContent` only when it fits existing layering.
- Add new overlays by updating `Component`, parameters, measure/provider logic, and DSL examples together.
- Add new tile sources under `mapSource/` or demo `customSources/`; return `TileResult`, not raw bitmaps/data.
- Test fastest target first: JVM demo/tests. For gesture changes, also check Android/iOS if behavior depends on pointer platform.
- Before publishing, update version in `KMaP/build.gradle.kts`; docs may still reference previous Maven Central version.

## References

- Website: https://kmap.rafambn.com/
- WASM demo: https://kmap.rafambn.com/kmapdemo/
- Repo: https://github.com/rafambn/KMaP
- Mapbox Style Spec: https://docs.mapbox.com/style-spec/
- Vector Tile Spec: https://github.com/mapbox/vector-tile-spec
