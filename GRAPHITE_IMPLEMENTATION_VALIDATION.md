# Graphite integration validation

Validation date: 2026-08-26.

## Baselines and branches

| Repository | Baseline | Branch |
| --- | --- | --- |
| KMaP | `3e34d792ab15029bf5a2579c938f5d4e5b57619a` | `feat/graphite-surface` |
| GraphiteSurface | `c70f010e7744f57f416280f6e567af6c6064842d` | `feat/kmap-graphite-integration` |
| Skiko fork | `e1fe36dff0a4fd3c293bd7b60cd4ef3b00ad1f01` | `feat/kmap-stroke-style` |

## Automated validation

The following commands pass with the sibling composite:

```shell
rtk ./gradlew -PgraphiteSurfacePath=../GraphiteSurface \
  :KMaP:jvmTest \
  :KMaP:compileAndroidMain \
  :KMaP:compileKotlinJs \
  :KMaP:compileKotlinWasmJs \
  :KMaP:compileKotlinIosSimulatorArm64 \
  --no-configuration-cache

rtk ./gradlew :DemoApp:androidApp:assembleDebug \
  -PgraphiteSurfacePath=../GraphiteSurface \
  --no-configuration-cache

rtk ./gradlew :KMaP:linkReleaseFrameworkIosSimulatorArm64 \
  -PgraphiteSurfacePath=../GraphiteSurface \
  --no-configuration-cache

rtk ./gradlew :KMaP:macosArm64Test \
  -PgraphiteSurfacePath=../GraphiteSurface \
  --no-configuration-cache

rtk ./gradlew :graphite-surface:jvmTest --no-configuration-cache
rtk ./gradlew :verifyGraphiteSurfaceBoundary \
  :graphite-surface:stageGraphiteWebRuntime \
  --no-configuration-cache

rtk ./gradlew :DemoApp:desktopApp:hotSnapshotJvmMain
```

The macOS test task has no test sources and is skipped after compilation and
link dependency resolution. It proves the native macOS fallback compiles
without substituting the iOS-only Skiko fork; it is not evidence of executed
tests.

`DemoApp:webApp:jsProcessResources` passes. Its processed resources contain the
Graphite worker, Graphite module and Wasm, and the fork's regular Skiko module
and Wasm. The staged and processed `skiko.wasm` SHA-256 values match.

The Desktop hot snapshot stores and reuses the configuration cache with the
sibling GraphiteSurface checkout detected automatically. The nested Skiko
composite exposes the JVM target's host class and native JARs without the
publication-only runtime variants competing during local variant selection.

The JVM tests initialize and close `GraphiteMapController`. A desktop smoke
test opened the forced `Graphite vector diagnostic` route and exercised zoom
and rotation without a runtime or rendering error. The fork retains the
`0.150.1` public type ABI expected by Compose while using the newer Graphite
module and native implementation.

## Open automated gates

- `:KMaP:check` and `:DemoApp:webApp:wasmJsBrowserDistribution` stop in the
  Kotlin Gradle plugin NPM resolver for the nested composite with
  `Included build 'GraphiteSurface' not found in build ':GraphiteSurface'`.
  JS/Wasm Kotlin compilation and Web resource staging pass. Validate the full
  distribution against published GraphiteSurface artifacts or after the
  composite resolver issue is fixed.
- `:graphite-surface:check` passes the adapter boundary check and JVM tests, but
  the iOS Simulator test executable fails to link
  `_OBJC_CLASS_$_GraphiteEngineGraphiteEngineView_iosKt`. The KMaP iOS
  Simulator framework itself links successfully.
- No clean consumer can resolve `com.rafambn:graphite-surface:0.1.0-SNAPSHOT`
  until GraphiteSurface and its fork artifacts are published.

## Manual gates not executed

- Android API 24/current device comparison and lifecycle testing.
- iOS Simulator/device visual, lifecycle and pixel-scale testing.
- JS/Wasm browser WebGPU, missing-WebGPU and device-loss testing.
- Compose-versus-Graphite screenshot comparison, including parent/child tile
  fallback. The JVM Graphite diagnostic itself was exercised successfully.
- Five-minute stress runs and performance/memory metrics.
- Stroke cap/join reference images.

`MapRenderBackend.Auto` selects Graphite on Android, iOS, JVM macOS/Linux, and
WebGPU browsers when the whole map is compatible. The manual gates above
remain open.
