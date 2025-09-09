const CACHE_NAME = 'postHere-v2';
const FILES_TO_CACHE = ['/', '/css/main.css', '/js/main.js'];

self.addEventListener('install', (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then((c) => c.addAll(FILES_TO_CACHE)));
    self.skipWaiting();
});
self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});
self.addEventListener('fetch', (event) => {
    event.respondWith(caches.match(event.request).then((r) => r || fetch(event.request)));
});

// Push
self.addEventListener('push', (event) => {
    if (!event.data) return;
    const payload = event.data.json(); // {type, notificationId, actor{nickname,profilePhotoUrl}, text}
    const title = payload.actor?.nickname || 'PostHere';
    const body = payload.text || '';
    const icon = payload.actor?.profilePhotoUrl || '/icons/icon-192.png';

    event.waitUntil(
        self.registration.showNotification(title, {
            body, icon, badge: icon,
            data: {url: '/notification?focus=' + (payload.notificationId || '')}
        })
    );
});

// 클릭 → 앱 포커스 + 라우팅
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const url = event.notification.data?.url || '/notification';
    event.waitUntil((async () => {
        const all = await clients.matchAll({type: 'window', includeUncontrolled: true});
        for (const c of all) {
            c.focus();
            c.postMessage({type: 'NAVIGATE', url});
            return;
        }
        await clients.openWindow(url);
    })());
});
