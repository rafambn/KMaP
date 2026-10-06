import {fileURLToPath} from 'node:url';
import {resolve} from 'node:path';
const projectRoot = fileURLToPath(new URL('../../', import.meta.url));
const roots = {
    js: resolve(projectRoot, "build/jsApp-browser"),
    wasm: resolve(projectRoot, "build/wasmApp-browser"),
    baseline: process.env.KMAP_BASELINE_DIR ? resolve(process.env.KMAP_BASELINE_DIR, "build/jsApp-browser") : null,
};
const instrumentation = `<script>
performance.setResourceTimingBufferSize(10000);
globalThis.pipelineTrace={workers:[],stalls:[],errors:[],logs:[],started:performance.now()};
const nativeLog=console.log;console.log=(...args)=>{pipelineTrace.logs.push({time:performance.now(),args:args.map(String)});nativeLog(...args)};
addEventListener("error",e=>pipelineTrace.errors.push({message:e.message,stack:e.error?.stack}));
addEventListener("unhandledrejection",e=>pipelineTrace.errors.push({message:String(e.reason),stack:e.reason?.stack}));
new PerformanceObserver(l=>pipelineTrace.stalls.push(...l.getEntries().map(e=>({start:e.startTime,duration:e.duration})))).observe({type:'longtask',buffered:true});
const NativeWorker=globalThis.Worker;
globalThis.Worker=class extends NativeWorker{
constructor(url,options){super(url,options);const record={url:String(url),start:performance.now(),messages:[],sent:[],terminated:false};this.record=record;pipelineTrace.workers.push(record);this.addEventListener('message',e=>{const m=e.data;record.messages.push({kind:m.kind,id:m.id,revision:m.revision,execution:m.execution,bytes:m.payload?.byteLength||0,time:performance.now(),message:m.message});});this.addEventListener('error',e=>{record.error=e.message});}
postMessage(m,transfers){const s={kind:m.kind,id:m.id,bytes:m.payload?.byteLength||0,time:performance.now(),zoom:m.zoom,row:m.row,col:m.col};this.record.sent.push(s);super.postMessage(m,transfers);s.detached=m.payload?.byteLength===0;}
terminate(){this.record.terminated=true;super.terminate();}}
</script>`;
Bun.serve({hostname:"127.0.0.1",port:8097,async fetch(request){
const url=new URL(request.url), parts=url.pathname.split('/').filter(Boolean);
if(parts[0]==='bench')return new Response(Bun.file(resolve(projectRoot, 'TileWorker/bench/run.mjs')));
if(parts[0]==='fixtures' && parts.length === 2 && /^[a-zA-Z0-9_]+\.pbf$/.test(parts[1])){
 const file=Bun.file(resolve(projectRoot, 'KMaP/testResources@jvm/tiles', parts[1]));
 return await file.exists()?new Response(file):new Response('Missing',{status:404});
}
const root=roots[parts.shift()];if(!root||parts.includes('..'))return new Response('Missing',{status:404});
const path=root+'/'+(parts.join('/')||'index.html'), file=Bun.file(path);
if(!(await file.exists()))return new Response('Missing',{status:404});
if(path.endsWith('/index.html'))return new Response((await file.text()).replace('</head>',instrumentation+'</head>'),{headers:{'Content-Type':'text/html'}});
return new Response(file);
}});

console.log("Tile pipeline demo: http://localhost:8097/js/?screen=vector&zoom=14");
