// Only explicitly listed static resources can enter the offline cache.
const CACHE = 'nannyapp-public-v11';
const ASSETS = ['./assets/css/style.css', './assets/js/app.js', './manifest.webmanifest', './assets/img/icon.svg'];
const allowed = new Set(ASSETS.map(path => new URL(path, self.registration.scope).href));
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(ASSETS)));
  self.skipWaiting();
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('nannyapp-') && key !== CACHE).map(key => caches.delete(key)))).then(() => self.clients.claim()));
});
self.addEventListener('message', event => {
  if (event.data?.type === 'LOGOUT') {
    event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('nannyapp-')).map(key => caches.delete(key)))));
  }
});
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' || !allowed.has(event.request.url)) return;
  event.respondWith(fetch(event.request).then(async response => {
    if (response.ok && !response.redirected && response.type === 'basic' && !/no-store|private/i.test(response.headers.get('Cache-Control') || '')) {
      // Offline storage is best-effort; its failure must not discard a good network response.
      try {
        const cache = await caches.open(CACHE);
        await cache.put(event.request, response.clone());
      } catch (_) {}
    }
    return response;
  }).catch(async () => {
    try {
      const cache = await caches.open(CACHE);
      const cached = await cache.match(event.request);
      if (cached) return cached;
    } catch (_) {}
    return Response.error();
  }));
});
