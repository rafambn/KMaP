# GraphiteSurface integration

KMaP uses GraphiteSurface for vector canvases through `MapRenderBackend.Auto`
when the Graphite backend is available. Raster canvases and map components that
are not part of the vector recording remain on the Compose path.

The integration uses the current `GraphiteEngine` API. Do not restore the old
`GraphiteRuntime`, `GraphitePaint`, or `GraphitePath` API from earlier
experiments.

The rendering split is:

```text
Compose/UI thread
    publishes immutable camera and scene snapshots

Graphite recorder workers
    record missing tile/layer entries under stable cache keys

Graphite presentation
    inserts cached recordings in Mapbox order with one camera transform
```

Camera-only changes must not re-record vector geometry. Pan, rotation, canvas
size, and magnifier scale are applied when cached recordings are inserted into
the frame. A recording is invalidated when its tile object, style layer, style
zoom, integer tile zoom, or tile dimensions change.

Scene requests use latest-value semantics. Recorder work may finish for an old
scene, but that scene is checked again before presentation and is discarded if
a newer snapshot exists. The renderer retains all recordings used by the
current scene plus a bounded recent cache.

GraphiteSurface frame insertion accepts a `GraphiteTransform`, rather than only
an integer translation, so reusable recordings can follow the camera without
encoding their paths again. This contract is shared by Android, JVM, JS, WASM,
iOS, and macOS implementations.

macOS Arm64 currently replays the immutable Graphite command program through a
Compose canvas because GraphiteSurface does not yet expose a native AppKit/Metal
presentation bridge. It still uses the same engine, cache keys, scheduling, and
camera transforms and must not be marked as an unsupported target.

Vector point/symbol overlays are still drawn by Compose over the Graphite
surface. Keep their ordering and coordinate conversion aligned with the vector
scene compiler when changing either path.
