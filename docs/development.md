# Development

Build the library for JVM first:

```shell
./kotlin build -m KMaP -p jvm
```

Run every configured check:

```shell
./kotlin check
```

See [Tile pipeline](tile-pipeline.md) for JS/Wasm worker packaging, runtime diagnostics and the reproducible preparation benchmark.

Preview the documentation locally:

```shell
mkdocs serve
```

Deploy the documentation:

```shell
mkdocs gh-deploy --force
```
