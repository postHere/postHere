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