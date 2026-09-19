const CACHE="dexter-ai-shell-v2";
const SHELL=["./","./index.html","./manifest.webmanifest","./app-icon.svg","./icon-192.png","./icon-512.png","./pwa.js"];
self.addEventListener("install",event=>{
  event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(SHELL)).then(()=>self.skipWaiting()));
});
self.addEventListener("activate",event=>{
  event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()));
});
self.addEventListener("fetch",event=>{
  const req=event.request;
  if(req.method!=="GET")return;
  const url=new URL(req.url);
  if(url.origin!==location.origin)return;
  event.respondWith(fetch(req).then(res=>{
    const copy=res.clone();
    caches.open(CACHE).then(cache=>cache.put(req,copy)).catch(()=>{});
    return res;
  }).catch(()=>caches.match(req).then(r=>r||caches.match("./index.html"))));
});