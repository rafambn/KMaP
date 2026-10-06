#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
output_dir=${1:?Usage: package-worker.sh browser-distribution-directory}
artifact_dir="$project_dir/build/artifacts/CompiledWebArtifact/TileWorkerjsrelease/kotlin-output"

# The application link must include the current KMaP processing implementation, even after cached builds.
rm -rf "$project_dir/build/tasks/_TileWorker_buildJsAppJsRelease" "$project_dir/build/artifacts/CompiledWebArtifact/TileWorkerjsrelease"
"$project_dir/kotlin" build -m TileWorker -p js -v release
test -f "$artifact_dir/TileWorker.mjs"
rm -rf "$output_dir/kmap-worker"
mkdir -p "$output_dir/kmap-worker"
cp -R "$artifact_dir/." "$output_dir/kmap-worker/"
mv "$output_dir/kmap-worker/TileWorker.mjs" "$output_dir/kmap-worker/tile-worker.mjs"

echo "KMaP processing worker: $output_dir/kmap-worker/tile-worker.mjs"
