# Tile pipeline

Each `MapState` owns one `CanvasEngine`. It coordinates registration, visible selections, normalized source requests, caches, fallbacks and publication. Fetches use structured coroutines. Encoded vector preparation uses two actual workers shared by the map: module Web Workers on JS/Wasm, owned single-thread executors on JVM/Android, and Kotlin/Native workers on iOS.

Workers decode protobuf, parse MVT geometry, evaluate source-layer/geometry/filter membership at integer tile zoom and return portable coordinates, part offsets, typed properties and layer indices. They retain their current preparation style, without retaining decoded tiles. The engine creates Compose geometry once per feature, then shares it across style layers. Existing drawing, paint/layout evaluation, clipping, text measurement, sprites and density handling remain on the map's UI thread.

## Sources and styles

Return uncompressed protobuf bytes through the existing `TileResult` contract:

```kotlin
val source: suspend (Int, Int, Int) -> TileResult<VectorTile> = { zoom, row, col ->
    val bytes = downloadMvt(zoom, row, col)
    TileResult.Success(EncodedVectorTile(zoom, row, col, bytes))
}
val style = requireNotNull(StyleResolver().resolve(styleJson).style)

vectorCanvas(
    parameters = VectorCanvasParameters(
        id = 1,
        tileSource = source,
        style = style,
    ),
)
```

Treat submitted preparation styles and payloads as immutable. The source gives the map ownership of the returned `ByteArray` and must stop mutating it. Coordinates must match the normalized request. Preserve coroutine cancellation in sources, and close owned HTTP clients when their owner is disposed. `DemoApp/shared/.../source/VectorTileSource.kt` shows this fetch-only path.

`StyleResolver` retains a `PreparationStyle` alongside rendering closures and resources. Paint, layout, visibility, fonts and sprites stay in `CompiledStyle`; they are never sent to workers. Changing only paint/layout reuses prepared tiles. Changing layer membership, filters or preparation locale advances the preparation revision and reuses retained raw bytes.

Manually built `CompiledStyle` values can supply an equivalent `preparation` definition. Keep that definition consistent with rendering layers and filters. Encoded tiles without it report a preparation failure. Existing sources returning decoded `VectorTile` remain supported through explicit local compatibility preparation, including custom non-MVT property values and closure filters. `compatibilityTiles` records that path; worker failure never silently switches to it.

Call `mapState.invalidateTiles(canvasId = 1)` after changing the content used by a registered tile source. It clears that canvas's raw/prepared caches and displayed tiles, cancels its fetches, rejects previous replies and reloads the current viewport without a camera change. Call `mapState.invalidateTiles()` to reload every canvas. Use the map's UI dispatcher; invalidation is asynchronous. If replacing the source callback, register the new callback before invalidating. Lambda identity is ignored, so recomposition can recreate callbacks without discarding tiles. Removing/recreating an ID or switching raster/vector source type creates a new generation. Canvas IDs must be unique.

`rememberMapState` closes the pipeline on disposal. Call `MapState.close()` when managing its lifecycle manually. Closing a map cancels owned fetches and closes workers without cancelling the supplied parent scope.

## Admission, caching and failures

The map admits six fetches and two preparations. Fetches plus waiting encoded payloads are capped at eight, and the completion channel holds eight results. Configuration and viewport channels are conflated. Admission orders demand by distance from the viewport center and shares fetch capacity between canvases. Pending obsolete preparation is discarded; accepted CPU work retains capacity until its terminal reply. A worker is terminated only for disposal or failure.

Per-canvas raw and prepared caches retain up to `maxCacheTiles`. Raw bytes have a 16 MiB budget; an individual encoded tile over 16 MiB is rejected. Prepared caches have a 32 MiB estimate based on coordinate/part arrays and property contents. These counters exclude graphics allocations, collection overhead and worker heaps. Active tiles and useful parent/child fallbacks may outlive cache membership. No persistent decoded cache or cross-canvas source sharing is added.

JS/Wasm copies retained input bytes into a separate typed buffer, transfers that buffer, and never accesses it again. Workers transfer a protobuf-encoded prepared result back. IDs and unsigned properties travel losslessly as tagged strings. Each reply is checked against its request/style revision; the engine additionally checks the canvas generation before publication. Invalidation advances that internal generation. Late valid viewport results can populate caches without restoring an obsolete selection.

Source failures retry on a subsequent viewport update, with two attempts while that tile remains demanded. A worker failure replaces its slot, reinstalls the required style and retries eligible current work once. Malformed preparation and conversion failures remain visible without automatic repeated retries. Other tiles and fallbacks remain usable.

Read `mapState.tilePipelineStatus(canvasId)` from the UI dispatcher. It reports admission counts, completed worker/compatibility tiles, retained byte estimates, elapsed stage totals and failure stage/message/attempts. Preparation time includes worker startup, style installation, transport and reply decoding; conversion time measures Compose object creation. Concurrent stage totals are sums of request times, not wall-clock latency. Conversion accepts one completed tile and yields before the next event; a single expensive tile can still block the UI.

## Browser packaging

```shell
rtk proxy ./DemoApp/jsApp/package-js-browser.sh
rtk proxy ./DemoApp/wasmApp/package-wasm-browser.sh
```

The distributions are `build/jsApp-browser` and `build/wasmApp-browser`. Both include `kmap-worker/tile-worker.mjs` and its emitted module dependencies. For another application, build/package its browser output, then run `TileWorker/package-worker.sh /absolute/distribution/path`. Deploy the entire worker directory beside the application's entry HTML. The worker URL resolves relative to `document.baseURI`, including deployments under a URL subpath.

Serve ES modules with a JavaScript MIME type and Wasm with `application/wasm`. A restrictive CSP must allow same-origin module scripts and `worker-src 'self'`. The protocol uses transferable `ArrayBuffer`, without requiring shared memory or cross-origin isolation. Missing assets, startup errors, protocol mismatches and processing timeouts are observable worker failures.

The Wasm packaging script also supplies Skiko, Compose resources and the emitted js-joda import, which the CLI output does not package automatically. Skiko's downloaded runtime is checksum-verified. Packaging forces a fresh application link because CLI 0.12 can reuse links after shared dependencies change. Workers start without a Compose application, DOM, Skiko initialization or canvas.

## Reproducing measurements

After packaging, run the local evidence server:

```shell
rtk proxy bun TileWorker/bench/serve.mjs
```

Open `http://localhost:8097/js/?screen=vector&zoom=14` or `/wasm/?screen=vector&zoom=14`. This optional route opens the existing remote vector demo around Berlin and logs pipeline diagnostics. The harness exposes `pipelineTrace` with worker message timing, transferred-buffer detachment, errors and main-thread long tasks. Instrumentation is confined to this local server.

Run the portable preparation comparison in that page's console:

```javascript
const {runPreparationBenchmark, fixtureStyle, fixtureTiles} = await import('/bench/run.mjs');
await runPreparationBenchmark({
    workerUrl: new URL('/js/kmap-worker/tile-worker.mjs', location.href).href,
    localModuleUrl: new URL('/js/kmap-worker/kotlin_TileWorker/LocalPreparationBenchmark.mjs', location.href).href,
    style: fixtureStyle,
    tiles: fixtureTiles,
    rounds: 24,
});
```

The same Kotlin algorithm runs locally, with one worker and with two workers. It alternates checked-in dense (118,884 bytes) and sparse (16,421 bytes) OHM fixtures. The fixture style covers their source layers without filters, allowing fills/outlines/symbol membership and shared geometry. The benchmark warms each backend, includes input copying, transport, serialization and identical-output checks, and records startup separately. A queued `MessageChannel` task measures UI delay; the threshold is 25% of the local baseline, selected before running worker variants.

Sample on October 5, 2026, in Chromium 141 headless on macOS arm64:

| Backend | 24 preparations | UI task delay | Worker startup |
| --- | ---: | ---: | ---: |
| Local | 272.8 ms | 273.0 ms | n/a |
| One worker | 285.2 ms | 0.1 ms | 51.4 ms |
| Two workers | 200.5 ms | 0.1 ms | 96.2 ms |

All paths returned 2,032,488 identical output bytes, and transferred input buffers detached. Both worker variants passed the 68.25 ms responsiveness threshold. One worker improved responsiveness without improving throughput. This sample supports two workers for preparation responsiveness; it is not an end-to-end frame-rate claim. JIT warmup, host load and cold module imports affect results. Raw results are in [the benchmark JSON](benchmarks/tile-preparation-browser.json).

The remote JVM demo completed nine tiles with 341.2 ms aggregate preparation, 26.5 ms Compose conversion, 403,243 retained raw bytes and 998,374 estimated prepared bytes. A separate cold JVM baseline completed the same nine-tile viewport with 67.8 ms protobuf/geometry decoding and 46.2 ms combined filtering/Path creation; fetch waits totalled 3,894.9 ms across concurrent requests. [JVM baseline samples](benchmarks/tile-demo-jvm-baseline.json) retain the stage measurements. The migrated cold JVM run took more preparation time; native throughput improvement is not established. Its executor ownership and real thread boundary are verified separately.

Android completed three initial portrait tiles and additional tiles after panning, with no failures. The Apple Silicon iOS simulator completed three initial tiles with 237.9 ms aggregate preparation and 10.0 ms conversion. These are runtime smoke checks on different devices/viewports, not comparative platform benchmarks.

The existing JS demo at baseline commit `2d4138c` produced six long tasks of 52–241 ms in the T3 preview, while the migrated JS demo completed nine tiles through two dedicated workers. The preview was hidden, its screenshots failed and its automation host subsequently disconnected. Headless Chromium then verified both packaged targets under `/js/` and `/wasm/`, plus the original JS/Wasm baseline, with remote tiles, pan, wheel zoom and resizing. Wasm additionally received two-finger rotation/pinch input and rendered the rotated world view. All runs completed without application errors. Baseline and worker screenshots retain the same map geometry, layer appearance, sprites and labels; their FPS overlays differ.

Headless JS accepted six initial tiles with 8.9 ms aggregate Compose conversion; Wasm accepted six with 4.5 ms. After rotation/zoom changes, Wasm completed 19 tiles with 17.7 ms aggregate conversion, while long UI tasks still reached 3.3 seconds with software drawing. Together with successful off-thread preparation, this points to the remaining renderer/paint/text work as the next bottleneck. Conversion yields after each tile, but offloading preparation does not make drawing asynchronous.

During these viewport changes, diagnostics peaked at six fetches, four waiting preparations and two active preparations. Wasm retained at most 5.3 MB of raw cache data and 2.8 MB of estimated prepared data. Active/fallback graphics and worker heaps are additional memory. Style/source changes, explicit invalidation, cancellation, failure recovery, queue saturation and geometry/filter transport parity are covered by the 411-test JVM suite, including real stored fixtures and owned-thread cleanup.

See [runtime traces](benchmarks/tile-demo-browser.json) and the [JS baseline](benchmarks/js-baseline.png), [JS worker](benchmarks/js-workers.png), and [Wasm worker](benchmarks/wasm-workers.png) captures. Cold startup, network variability, JIT warmup and software drawing prevent a general end-to-end speedup claim from these smoke checks. Worker heaps are excluded from `performance.memory`; foreground hardware-rendered frame-rate and memory profiling remain necessary before making renderer performance claims.

For native runtime reproduction, launch desktop with `./kotlin run -m desktopApp -p jvm -- --vector`, iOS with the `--vector` launch argument, or Android with the boolean `vector` intent extra. Normal demo entry keeps its existing world view. `iosX64` remains disabled.
