#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/../.." && pwd)
output_dir="$project_dir/build/wasmApp-browser"
artifact_dir="$project_dir/build/artifacts/CompiledWebArtifact/wasmAppwasmJsrelease"

# CLI 0.12 can reuse an application link after a dependency changes. Re-link packaged applications.
rm -rf "$project_dir/build/tasks/_wasmApp_buildWasmJsAppWasmJsRelease" "$artifact_dir"
"$project_dir/kotlin" build -m wasmApp -p wasmJs -v release
test -d "$artifact_dir"

rm -rf "$output_dir"
mkdir -p "$output_dir"
cp -R "$artifact_dir/." "$output_dir/"

# The CLI emits browser imports but does not package Skiko or resolve npm specifiers.
runtime_dir=$(mktemp -d)
trap 'rm -rf "$runtime_dir"' EXIT
runtime_version="0.150.1"
runtime_sha256="aff08515ebd22863e9bb0b776068dfd2c7f7a384ed5e7e480b2ded349b653a0c"
curl --fail --silent --show-error --location \
    "https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-js-wasm-runtime/$runtime_version/skiko-js-wasm-runtime-$runtime_version.jar" \
    --output "$runtime_dir/skiko.jar"
printf '%s  %s\n' "$runtime_sha256" "$runtime_dir/skiko.jar" | sha256sum --check --status -
(
    cd "$runtime_dir"
    jar xf skiko.jar
)
cp "$runtime_dir/skiko.mjs" "$runtime_dir/skiko.wasm" "$runtime_dir/js-reexport-symbols.mjs" "$output_dir/kotlin-output/"
npm_dir="$project_dir/build/tasks/_wasmApp_npmInstallWasmJs/node_modules/@js-joda/core"
cp "$npm_dir/dist/js-joda.esm.js" "$output_dir/kotlin-output/js-joda.mjs"
cp "$npm_dir/LICENSE" "$output_dir/kotlin-output/js-joda.LICENSE"
sed -i.bak "s|from '@js-joda/core'|from './js-joda.mjs'|" "$output_dir/kotlin-output/wasmApp.import-object.mjs"
rm "$output_dir/kotlin-output/wasmApp.import-object.mjs.bak"
cp "$script_dir/resources/index.html" "$output_dir/index.html"
sed -i.bak 's|{{kotlin.scripts}}|<script type="module" src="kotlin-output/wasmApp.mjs"></script>|' "$output_dir/index.html"
rm "$output_dir/index.html.bak"
cp "$script_dir/resources/styles.css" "$output_dir/styles.css"
mkdir -p "$output_dir/composeResources/kmap.kmapdemo.generated.resources"
cp -R "$project_dir/DemoApp/shared/composeResources/." "$output_dir/composeResources/kmap.kmapdemo.generated.resources/"
"$project_dir/TileWorker/package-worker.sh" "$output_dir"

echo "Kotlin/Wasm browser distribution: $output_dir"
