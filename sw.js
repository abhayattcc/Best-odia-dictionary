const cacheName = 'kids-word-adventure-cache-v3';
const filesToCache = [
    './',
    './index.html',
    './js/sql-wasm.min.js',
    './js/sql-wasm.wasm',
    './js/pdf.min.js',
    './js/pako.min.js',
    './js/tesseract.min.js',
    './js/worker.min.js',
    './js/tesseract-core.wasm.js',
    './lang-data/eng.traineddata.gz',
    './lang-data/hin.traineddata.gz',
    './lang-data/ori.traineddata.gz',
    './data/english_odia.db.gz',
    './data/odia_meaning.db.gz',
    './data/english_hindi.db.gz'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(cacheName).then((cache) => {
            return cache.addAll(filesToCache).then(() => {
                self.skipWaiting();
            });
        })
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames
                    .filter((name) => name !== cacheName)
                    .map((name) => caches.delete(name))
            );
        }).then(() => {
            return self.clients.claim();
        })
    );
});

self.addEventListener('fetch', (event) => {
    event.respondWith(
        caches.match(event.request).then((response) => {
            if (response) {
                return response;
            }
            return fetch(event.request).then((networkResponse) => {
                if (!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic') {
                    return networkResponse;
                }
                const responseToCache = networkResponse.clone();
                caches.open(cacheName).then((cache) => {
                    cache.put(event.request, responseToCache);
                });
                return networkResponse;
            }).catch(() => {
                return caches.match('./index.html');
            });
        })
    );
});