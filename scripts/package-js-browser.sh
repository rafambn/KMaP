#!/usr/bin/env sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
artifact_root="$project_dir/build/artifacts/CompiledWebArtifact"
output_dir="$project_dir/build/jsApp-browser"
runtime_version="0.144.6"
runtime_sha256="233ca8fe757358d54b2bcc70c1d76c1a1d325b0e40c11371e66165e57ee060ed"
runtime_url="https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-js-wasm-runtime/$runtime_version/skiko-js-wasm-runtime-$runtime_version.jar"

"$project_dir/kotlin" build -m jsApp -p js -v release

artifact_dir=$(find "$artifact_root" -maxdepth 1 -type d -name "jsAppjsrelease" -print -quit)
if [ -z "$artifact_dir" ] || [ ! -d "$artifact_dir/kotlin-output" ]; then
    echo "Kotlin/JS output was not found under $artifact_root" >&2
    exit 1
fi

runtime_dir=$(mktemp -d)
trap 'rm -rf "$runtime_dir"' EXIT

curl --fail --silent --show-error --location "$runtime_url" --output "$runtime_dir/skiko.jar"
printf '%s  %s\n' "$runtime_sha256" "$runtime_dir/skiko.jar" | sha256sum --check --status -

(
    cd "$runtime_dir"
    jar xf skiko.jar
)

# This is a generated directory. It is safe to replace on each packaging run.
rm -rf "$output_dir"
mkdir -p "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skia/impl"
mkdir -p "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skiko/wasm"

cp -R "$artifact_dir/kotlin-output/." "$output_dir/kotlin-output/"
cp "$runtime_dir/skiko.mjs" "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skia/impl/skiko.mjs"
cp "$runtime_dir/skiko.wasm" "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skia/impl/skiko.wasm"
cp "$runtime_dir/js-reexport-symbols.mjs" \
    "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skiko/wasm/js-reexport-symbols.mjs"
cp "$project_dir/DemoApp/jsApp/resources/skiko-wasm-reexport.mjs" \
    "$output_dir/kotlin-output/skiko-kjs/org/jetbrains/skiko/wasm/skiko.mjs"

mkdir -p "$output_dir/composeResources/kmap.kmapdemo.generated.resources"
cp -R "$project_dir/DemoApp/shared/composeResources/." \
    "$output_dir/composeResources/kmap.kmapdemo.generated.resources/"
cp "$project_dir/DemoApp/jsApp/resources/index.html" "$output_dir/index.html"
cp "$project_dir/DemoApp/jsApp/resources/styles.css" "$output_dir/styles.css"

echo "Kotlin/JS browser distribution: $output_dir"
