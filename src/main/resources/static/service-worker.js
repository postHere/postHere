const CACHE_NAME = 'postHere-v1';
const FILES_TO_CACHE = [
    '/',
    '/forumMain',
    '/css/main.css',
    '/js/main.js'
];

// 서비스 워커 설치 시 파일 캐싱
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(FILES_TO_CACHE);
        })
    );
});

// 네트워크 요청 시 캐시에서 먼저 찾아보고, 없으면 네트워크에서 가져오기
self.addEventListener('fetch', (event) => {
    event.respondWith(
        caches.match(event.request).then((response) => {
            return response || fetch(event.request);
        })
    );
});
// OS 푸시 수신 → 웹 알림 표시
self.addEventListener('push', (event) => {
    let payload = {};
    try {
        payload = event.data ? event.data.json() : {};
    } catch (e) {
        try {
            payload = JSON.parse(event.data.text());
        } catch (ignore) {
        }
    }

    const DEFAULT_ICON = 'https://posthere-s3.s3.ap-northeast-2.amazonaws.com/icon/tmp192.png';
    const DEFAULT_BADGE = 'https://posthere-s3.s3.ap-northeast-2.amazonaws.com/icon/tmp192.png';

    const title = payload.title || 'PostHere';
    const body = payload.body || '새 알림이 도착했어요';
    const icon = payload.icon || DEFAULT_ICON;
    const badge = payload.badge || DEFAULT_BADGE;
    const url = payload.url || '/notification'; // 알림 클릭 시 이동할 경로

    const options = {
        body,
        icon,
        badge,
        data: {url},                 // 클릭 시 참조할 URL
        tag: payload.tag || 'posthere-noti',
        renotify: false,
        requireInteraction: false,
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

// 알림 클릭 → 기존 탭 포커스 or 새 창으로 이동
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const targetUrl = event.notification.data?.url || '/notification';

    event.waitUntil((async () => {
        const allClients = await self.clients.matchAll({type: 'window', includeUncontrolled: true});

        // 이미 열려 있는 같은 오리진 탭이 있으면 포커스 + 라우팅 메시지
        for (const client of allClients) {
            const url = new URL(client.url);
            if (url.origin === self.location.origin) {
                client.postMessage({type: 'NAVIGATE', url: targetUrl});
                return client.focus();
            }
        }
        // 없으면 새 창 오픈
        return self.clients.openWindow(targetUrl);
    })());
});