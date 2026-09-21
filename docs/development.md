# Development

Build the library for JVM first:

```shell
./kotlin build -m KMaP -p jvm
```

Run every configured check:

```shell
./kotlin check
```

Preview the documentation locally:

```shell
mkdocs serve
```

Deploy the documentation:

```shell
mkdocs gh-deploy --force
```
