# Future GraphiteSurface integration

Status: parked until GraphiteSurface has a stable runtime, recorder manager,
and native render-thread model. Do not add another KMaP rendering experiment
before those contracts exist.

KMaP should initially use one `GraphiteSurface` composable and one presentation
target. The map does not need multiple windows or presentation targets to
parallelize its work.

The intended future split is:

```text
Compose/UI thread
    publishes camera and immutable scene changes

CPU preparation workers
    decode tiles, build geometry, shape labels, prepare transferable data

Graphite recorder workers
    record independent work with stable tile/layer keys

Graphite render thread
    assembles a complete frame in Mapbox layer order, submits, and presents
```

Requirements KMaP will place on GraphiteSurface:

- camera input uses latest-value semantics so gesture events do not build a
  backlog;
- recorder and preparation queues are bounded;
- obsolete tile, zoom, and camera work can be cancelled or discarded;
- recorder completion order never changes Mapbox visual order;
- the render thread never waits for a late tile or recorder;
- an old complete scene remains drawable while a newer scene is incomplete;
- tile and layer recordings have stable identities for reuse;
- text atlas and GPU resources do not reset on every camera update;
- diagnostics expose UI time, queue wait, record time, submit time, GPU
  completion, queue depth, and dropped work;
- Android, Apple, and JVM use dedicated native threads; Web uses Web Workers.

The exact public API is intentionally not specified here. GraphiteSurface must
first decide whether users submit typed scene data, portable command buffers,
or code registered inside each worker. That choice determines what KMaP can
share across JVM, Kotlin/Native, JS, and Wasm.

The earlier main-thread canvas experiments are not the architecture. Their
results only showed that reducing Compose invalidation helps but cannot remove
gesture stutter while recording and presentation remain coupled to the UI
thread.
