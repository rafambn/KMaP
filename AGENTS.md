# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Overview

**KMaP** is a flexible, high-performance Compose Multiplatform mapping library that enables cross-platform map rendering and interaction. It supports Android, iOS, JVM/Desktop, JavaScript, WebAssembly, and Node.js through a single Kotlin codebase.

The library version is **0.3.1** and is published to Maven Central as `com.rafambn:KMaP`.

## Build System & Commands

### Build Configuration

- **Build System**: Gradle 8.0+ with Kotlin DSL
- **Kotlin Version**: 2.2.0 with JVM Toolchain 17
- **Target Platforms**: Android (minSdk 24), iOS, JVM, JS, WASM, Node.js

### Common Gradle Commands

```bash
# Build library (all platforms)
./gradlew build

# Build specific targets
./gradlew KMaP:build                    # Build KMaP library
./gradlew KMaPDemo:build                # Build demo app

# Build specific platform
./gradlew KMaP:assembleAndroidRelease   # Android release
./gradlew KMaP:jsBrowserProductionWebpack  # JavaScript/browser
./gradlew KMaPDemo:wasmJsBrowserDistribution # WebAssembly/browser (outputs to mkdocs/docs/kmapdemo/)
./gradlew KMaP:linkReleaseFrameworkIosArm64  # iOS framework

# Run demo app
./gradlew KMaPDemo:run                  # Desktop (JVM) demo
./gradlew KMaPDemo:wasmJsBrowserRun     # WASM demo in browser

# Testing & quality
./gradlew test                          # Run tests
./gradlew testDebugUnitTest             # Android unit tests
./gradlew check                         # Code analysis

# Publishing (Maven Central)
./gradlew publish                       # Requires Maven Central credentials
./gradlew publishToMavenCentral         # Publish release
```

### Development Workflow

```bash
# Clean build
./gradlew clean build

# Incremental development (KMaP changes)
./gradlew KMaP:build -x test            # Quick build without tests

# Watch for changes (JS/WASM)
./gradlew -t KMaPDemo:wasmJsBrowserDevelopmentRun

# Desktop app development
./gradlew KMaPDemo:run --continuous
```

## Architecture Overview

### High-Level Structure

KMaP uses a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────┐
│  Composable UI Layer                │ KMaP.kt (main entry point)
├─────────────────────────────────────┤
│  Component System                   │ Markers, Clusters, Canvas, Paths
├─────────────────────────────────────┤
│  Camera & State Management          │ MapState, CameraState
├─────────────────────────────────────┤
│  Gesture & Motion Control           │ MotionController, Gesture Detection
├─────────────────────────────────────┤
│  Tile Rendering Engine              │ CanvasEngine, TileRenderer, TileSource
├─────────────────────────────────────┤
│  Coordinate Systems & Utils         │ Reference types, Math, Projections
```

### Core Data Flow

1. **User Input** → Gesture Detection (platform-specific touch/mouse)
2. **Motion Control** → MotionController processes input, updates MapState
3. **MapState Change** → Triggers viewport/camera updates
4. **Tile Visibility** → CanvasEngine calculates visible tile bounds via TileFinder
5. **Tile Loading** → TileRenderer requests tiles asynchronously from TileSource
6. **Component Positioning** → Components positioned in tile space, rendered in screen space via Compose LazyLayout
7. **Rendering** → Tiles + overlays composed into final map view

### Module Organization

```
KMaP/
├── KMaP/                 # Library module (published to Maven Central)
│   └── src/commonMain/
│       └── com/rafambn/kmap/
│           ├── core/          # Map engine (KMaP.kt, MapState, CameraState, MotionController)
│           ├── tiles/         # Tile system (TileSource, TileRenderer, CanvasEngine)
│           ├── components/    # UI overlays (Markers, Clusters, Canvas, Path)
│           ├── gestures/      # Input handling (Gesture detection, touch/mouse)
│           ├── mapProperties/ # Configuration (Zoom ranges, bounds, projections)
│           ├── mapSource/     # Tile implementations (Raster, Vector, GeoJSON)
│           └── utils/         # Utilities (Coordinates, Math, Styles, MVT parsing)
│
├── KMaPDemo/             # Demo application (all platforms)
├── Kflate/               # Compression utility (GZIP/Deflate)
└── iosApp/               # Native iOS wrapper
```

### Key Architectural Patterns

#### Type-Safe Coordinate System

KMaP uses sealed coordinate types to prevent bugs:
- **`Coordinates`**: Geographic (longitude, latitude)
- **`ProjectedCoordinates`**: Projected geographic space
- **`TilePoint`**: Internal tile space (0-1 normalized to tile)
- **`ScreenOffset`**: Screen pixels from viewport top-left
- **`DifferentialScreenOffset`**: Delta values for transformations
- **`CanvasDrawReference`**: Canvas drawing coordinates

Transformations between types use overloaded operators (+, -, *, /) on the `Reference` base class, ensuring type safety.

#### State Management

- **`CameraState`**: Immutable state of camera position, zoom, rotation (read-only at any moment)
- **`MapState`**: Mutable wrapper holding current camera state and map configuration
- **State changes** trigger minimal Compose recompositions via `mutableStateOf`

#### Tile Rendering Strategy

- **Viewport-driven**: Only visible tiles are requested
- **Multi-level caching**: Maintains tiles at multiple zoom levels for smooth zooming
- **Async loading**: Tiles fetched via coroutines, non-blocking
- **Bitmap pooling**: Rendered tiles cached with configurable max size

#### Component System

Components are positioned in **tile space** but rendered in **screen space**:
```kotlin
sealed interface Component
├── Marker(parameters, @Composable content)
├── Cluster(parameters, @Composable content)
├── Canvas(parameters, @Composable content)
└── Path(parameters, @Composable content)
```

Allows flexible UI customization while maintaining coordinate transformations.

#### Motion Control DSL

```kotlin
// Immediate movement
motionController.move {
    positionBy(offset)
    zoomBy(amount)
    rotateBy(degrees)
}

// Animated movement
motionController.animate {
    positionTo(target, animationSpec)
    zoomTo(level, animationSpec)
    rotateTo(angle, animationSpec)
}
```

### Tile Source Interface

All tile providers implement a common interface:
```kotlin
fun interface TileSource {
    suspend fun getTile(tilePoint: TilePoint, mapProperties: MapProperties): ImageBitmap?
}
```

Supports:
- **Raster tiles**: Online (XYZ, WMTS) or cached
- **Vector tiles**: Mapbox Vector Tile (MVT) format with Mapbox/MapLibre style specification
- **GeoJSON**: Non-tiled GeoJSON sources with client-side rendering

### Vector Tile & Style Support

- **MVT Format**: Protobuf-based Mapbox Vector Tile parsing (`MVTile`, `RawMVTile`)
- **Styles**: Mapbox/MapLibre style specification JSON support (sources, layers, paint/layout properties)
- **Serialization**: Kotlinx Serialization with JSON and Protobuf support

## Key Directories & Files

| Path | Purpose |
|------|---------|
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/KMaP.kt` | Main Composable entry point |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/MapState.kt` | Map state container and re-export |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/CameraState.kt` | Camera position/zoom/rotation data |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/core/MotionController.kt` | Animation and movement control |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/tiles/TileRenderer.kt` | Async tile rendering with caching |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/tiles/CanvasEngine.kt` | Tile visibility management |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/Components.kt` | Marker, Cluster, Canvas, Path types |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/components/KMaPContent.kt` | Component DSL builder |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/utils/ReferenceUtils.kt` | Coordinate type definitions |
| `KMaP/src/commonMain/kotlin/com/rafambn/kmap/utils/style/Style.kt` | Mapbox style deserialization |
| `KMaPDemo/` | Demo application showcasing all features |

## Important Implementation Details

### Multiplatform Considerations

- **No platform-specific code in commonMain**: All platform differences handled via dependency injection
- **Platform-specific HTTP clients**: OkHttp (Android/JVM), Darwin (iOS), JS (Web), configured per platform
- **WASM experimental**: Full support for modern browser environments with `wasmJs` target
- **iOS static frameworks**: Compiled as static libraries for integration into native projects

### Performance Optimizations

1. **LazyLayout**: Renders only visible UI components
2. **Viewport-based tile loading**: Only requests tiles that appear in current view
3. **Multi-level tile caching**: Speeds up zoom animations by maintaining tiles at nearby zoom levels
4. **Coroutine-based async**: Non-blocking tile fetching prevents UI stutter
5. **Bitmap pooling**: Configurable cache for rendered tiles avoids excessive memory allocation

### Testing

- 46 well-organized Kotlin files with clear separation of concerns
- Math operations in `MathUtils.kt` are stateless and easily testable
- Gesture detection logic isolated in `MapGesture.kt`
- Test resources in `commonTest/resources/`

## Development Tips

### Starting a New Feature

1. Determine which layer(s) your feature affects (see High-Level Structure)
2. Add state to `MapState.kt` if needed
3. Implement logic in appropriate module (tiles, components, gestures, etc.)
4. Add Compose UI in the component or tiles layer
5. Test on at least one platform (Desktop JVM is fastest for iteration)

### Common Tasks

**Add a new tile source**:
- Implement `TileSource` interface in `mapSource/` directory
- Call `suspend fun getTile()` with caching logic
- Add example in `KMaPDemo` app

**Add a new component type**:
- Add sealed subtype to `Component` in `components/Components.kt`
- Implement positioning logic (coordinate transformation)
- Create example in `KMaPContent.kt` DSL

**Modify gesture handling**:
- Edit `gestures/MapGesture.kt` or `gestures/PathGesture.kt`
- Test on Android/iOS for platform-specific behavior
- Update `detectMapGestures()` signature if adding new callbacks

**Work with vector tiles**:
- Review `utils/vectorTile/MVTile.kt` for data model
- Update `utils/style/Style.kt` for style features
- Reference Mapbox spec: https://github.com/mapbox/vector-tile-spec

### Debugging Tips

- **MapState changes**: Add logging to `MapState.kt` to trace camera updates
- **Tile loading**: Check `TileRenderer.kt` and `CanvasEngine.kt` for viewport calculations
- **Gesture detection**: Enable debug logs in `MapGesture.kt` to trace pointer events
- **Coordinate transforms**: Test `ReferenceUtils.kt` conversions with known coordinates

### Platform-Specific Debug

**Android**: Use Android Studio debugger on demo app
**iOS**: Use Xcode with KMaPDemo iOS target
**Desktop**: Run `./gradlew KMaPDemo:run` and attach IDE debugger
**Web/WASM**: Use browser DevTools on `http://localhost:8080/kmapdemo`

## Multiplatform Publishing

KMaP is published to Maven Central via vanniktech maven-publish plugin:

```gradle
coordinates(
    groupId = "com.rafambn",
    artifactId = "KMaP",
    version = "0.3.1"
)
```

**Publishing requires**:
- GPG signing configured
- Maven Central credentials in `~/.gradle/gradle.properties`
- Command: `./gradlew publishToMavenCentral`

## Documentation

- **Project Website**: https://kmap.rafambn.com/
- **WASM Demo**: https://kmap.rafambn.com/kmapdemo/
- **GitHub Repository**: https://github.com/rafambn/kmap
- **Mapbox Style Spec**: https://docs.mapbox.com/style-spec/
- **Vector Tile Spec**: https://github.com/mapbox/vector-tile-spec
