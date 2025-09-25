/**
 * Service Worker for PostHere
 *
 * 역할 개요
 * - 오프라인/저대역폭 환경을 위한 정적 리소스 캐싱
 * - Web Push 수신/표시 및 클릭 시 클라이언트 라우팅
 *
 * 동작 흐름
 * 1) install: 미리 지정한 파일 리스트(FILES_TO_CACHE)를 열고 캐시에 저장
 * 2) activate: 즉시 활성화(self.clients.claim)로 기존 탭에도 적용
 * 3) fetch: Cache-First 전략(캐시에 있으면 사용, 없으면 네트워크)
 * 4) push: 서버에서 수신한 payload(JSON)를 Notification으로 표시
 * 5) notificationclick: 이미 열린 창이 있으면 focus + postMessage로 라우팅,
 *    없으면 새 창 오픈
 *
 * 확장 포인트(코드 변경 없이 개념 설명)
 * - 캐시 무결성: activate에서 이전 버전 캐시 삭제(CACHE_NAME 비교)
 * - 전략 다양화: 라우트/자원 유형에 따라 SWR/Stale-While-Revalidate 등 도입
 * - 푸시 UX: actions, tag, renotify, requireInteraction, silent 등 옵션
 * - 보안/안정성: push payload 파싱 try/catch, 데이터 검증, 실패 핸들링
 */

const CACHE_NAME = 'postHere-v2';
const FILES_TO_CACHE = ['/css/main.css', '/js/main.js'];

self.addEventListener('install', (event) => {
    // [수정] addAll이 하나라도 404면 install 자체가 실패 → 일부 실패는 로깅만 하고 계속 진행
    event.waitUntil((async () => {
        try {
            const cache = await caches.open(CACHE_NAME);
            const tasks = FILES_TO_CACHE.map(url =>
                cache.add(url).catch(err => {
                    // 해당 리소스만 건너뛰고 나머지는 계속 캐싱
                    console.warn('[SW] cache.add failed:', url, err && err.message);
                })
            );
            await Promise.allSettled(tasks);
        } catch (e) {
            // install 전체가 막히지 않도록 마지막 방어
            console.warn('[SW] install cache phase skipped:', e && e.message);
        }
    })());
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
    event.respondWith(caches.match(event.request).then((r) => r || fetch(event.request)));
});

// [수정] 안전 파서: async/await으로 event.data.text() 처리 → DevTools "Push" 빈 페이로드 시 에러 방지
async function safeParse(event) {
    try {
        if (!event.data) return {};
        const txt = await event.data.text();
        try {
            return JSON.parse(txt);
        } catch {
            return {body: txt};
        }
    } catch {
        return {};
    }
}

// Push
self.addEventListener('push', (event) => {
    // [수정] safeParse를 async로 처리 → 반드시 event.waitUntil 안에서 await
    event.waitUntil((async () => {
        const payload = await safeParse(event);

        // 기대 payload(예): {type, notificationId, actor{nickname,profilePhotoUrl}, text}
        // [수정] 기본값 보강 → 항상 OS 알림이 뜨도록 함
        const title = (payload.actor && payload.actor.nickname) || payload.title || 'PostHere';
        const body = payload.text || payload.body || '새 알림이 도착했습니다.';
        const icon = (payload.actor && payload.actor.profilePhotoUrl) || payload.icon || '/icons/icon-192.png';
        const badge = payload.badge || '/icons/badge-72.png';
        const url = payload.url || ('/notification?focus=' + (payload.notificationId || ''));

        await self.registration.showNotification(title, {
            body,
            icon,
            badge,
            tag: 'posthere-noti',
            renotify: true,
            data: {url, ...(payload.data || {})}
        });

        // (선택) 열린 탭에 브로드캐스트 → 뱃지/목록 갱신은 페이지 스크립트가 처리
        const clientsList = await self.clients.matchAll({type: 'window', includeUncontrolled: true});
        for (const client of clientsList) client.postMessage({type: 'NOTIFICATION_NEW', payload});
    })());
});

// 클릭 → 앱 포커스 + 라우팅
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const url = event.notification.data?.url || '/notification';
    event.waitUntil((async () => {
        const all = await clients.matchAll({type: 'window', includeUncontrolled: true});
        for (const c of all) {
            try {
                await c.focus();
            } catch {
            }
            c.postMessage({type: 'NAVIGATE', url});
            return;
        }
        await clients.openWindow(url);
    })());
});
