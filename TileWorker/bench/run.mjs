// Run from the collaborative browser console using absolute URLs for the packaged worker and fixtures.
export const fixtureStyle = {
    locale: "pt",
    layers: [
        "land_ohm_lines", "place_areas", "place_points_centroids", "water_areas", "water_areas_centroids",
        "water_lines", "transport_areas", "transport_lines", "route_lines", "transport_points_centroids",
        "amenity_areas", "amenity_points_centroids", "buildings", "buildings_points_centroids",
        "landuse_areas", "landuse_points_centroids", "landuse_lines", "other_areas", "other_points_centroids", "other_lines",
    ].flatMap(sourceLayer => ["fill", "line", "symbol"].map(type => ({id: sourceLayer + "-" + type, type, sourceLayer}))),
};
export const fixtureTiles = [
    {url: "/fixtures/ohm_14_8800_5374.pbf", zoom: 14, row: 5374, col: 8800},
    {url: "/fixtures/ohm_16_35200_21496.pbf", zoom: 16, row: 21496, col: 35200},
];

export async function runPreparationBenchmark({workerUrl, localModuleUrl, style, tiles, rounds = 12}) {
    const module = await import(localModuleUrl);
    const createLocal = Object.values(module).find(value => typeof value === "function");
    const local = createLocal(JSON.stringify(style));
    const inputs = await Promise.all(tiles.map(async tile => ({...tile, bytes: await (await fetch(tile.url)).arrayBuffer()})));
    const reports = [];
    const marker = () => {
        const channel = new MessageChannel();
        const start = performance.now();
        const promise = new Promise(resolve => {
            channel.port1.onmessage = () => { channel.port1.close(); channel.port2.close(); resolve(performance.now() - start); };
        });
        channel.port2.postMessage(0);
        return promise;
    };
    // Warm the common algorithm before measuring a burst.
    for (const tile of inputs) tile.expected = local(tile.bytes, tile.zoom, tile.row, tile.col);
    let start = performance.now();
    let responsive = marker();
    let outputBytes = 0;
    for (let index = 0; index < rounds; index++) {
        const tile = inputs[index % inputs.length];
        const result = local(tile.bytes, tile.zoom, tile.row, tile.col);
        assertOutput(tile.expected, result);
        outputBytes += result.byteLength;
    }
    const localElapsed = performance.now() - start;
    const localMarker = await responsive;
    // Choose the responsiveness threshold from the local baseline, before running worker variants.
    reports.push({backend: "local", rounds, elapsedMs: localElapsed, markerDelayMs: localMarker, outputBytes});
    const markerThresholdMs = localMarker * 0.25;
    for (const count of [1, 2]) {
        const workers = [];
        const startup = performance.now();
        try {
            for (let index = 0; index < count; index++) {
                const worker = new Worker(workerUrl, {type: "module"});
                workers.push(worker);
                await receive(worker, () => true, () => {});
                await receive(worker, m => m.kind === "style-ready", () => worker.postMessage({
                    kind: "install-style", revision: "bench", definition: JSON.stringify(style),
                }));
            }
            const startupMs = performance.now() - startup;
            // One warm-up job per worker includes transport and output serialization.
            await Promise.all(workers.map((worker, index) => {
                const tile = inputs[index % inputs.length], payload = tile.bytes.slice(0);
                return receive(worker, m => m.kind === "prepared", () => worker.postMessage({
                    kind: "prepare", id: "warm", revision: "bench", zoom: tile.zoom, row: tile.row, col: tile.col, payload,
                }, [payload]));
            }));
            start = performance.now();
            responsive = marker();
            let next = 0;
            let preparedBytes = 0;
            let detached = true;
            await Promise.all(workers.map(async worker => {
                while (next < rounds) {
                    const index = next++, tile = inputs[index % inputs.length], payload = tile.bytes.slice(0);
                    const result = await receive(worker, m => m.kind === "prepared" && m.id === String(index), () => {
                        worker.postMessage({kind: "prepare", id: String(index), revision: "bench",
                            zoom: tile.zoom, row: tile.row, col: tile.col, payload}, [payload]);
                        detached = detached && payload.byteLength === 0;
                    });
                    if (result.execution !== "dedicated-worker") throw new Error("Unexpected execution location");
                    assertOutput(tile.expected, result.payload);
                    preparedBytes += result.payload.byteLength;
                }
            }));
            const elapsedMs = performance.now() - start;
            const markerDelayMs = await responsive;
            if (preparedBytes !== outputBytes) throw new Error("Local and worker output sizes differ");
            reports.push({backend: count + " workers", rounds, startupMs, elapsedMs, markerDelayMs,
                outputBytes: preparedBytes, identicalOutput: true, detached, responsivenessPassed: markerDelayMs <= markerThresholdMs});
        } finally { workers.forEach(worker => worker.terminate()); }
    }
    return {markerThresholdMs, inputs: inputs.map(tile => ({url:tile.url, bytes:tile.bytes.byteLength})), reports,
        note:"CPU/transport benchmark only. Compose conversion, drawing and text measurement are measured separately in the demo."};
}

function assertOutput(expected, actual) {
    const left = new Uint8Array(expected), right = new Uint8Array(actual);
    if (left.length !== right.length) throw new Error("Prepared output length differs");
    for (let index = 0; index < left.length; index++) {
        if (left[index] !== right[index]) throw new Error("Prepared output differs at byte " + index);
    }
}

function receive(worker, accept, submit) {
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => finish(new Error("Worker timed out")), 15000);
        const finish = (error, value) => {
            clearTimeout(timer);
            worker.removeEventListener("message", message);
            worker.removeEventListener("error", failed);
            error ? reject(error) : resolve(value);
        };
        const message = event => {
            const result = event.data;
            if (result.kind === "failed" || result.kind === "fatal") finish(new Error(result.message));
            else if (accept(result)) finish(null, result);
        };
        const failed = event => finish(new Error(event.message));
        worker.addEventListener("message", message);
        worker.addEventListener("error", failed);
        submit();
    });
}
