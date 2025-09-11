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

/**
 * [알림 클릭 → 앱 포커스 + 특정 URL로 이동]
 * - 이 SW의 'notificationclick'에서:
 *   1) 이미 열린 창이 있으면 focus()
 *   2) 해당 창으로 postMessage({ type: 'NAVIGATE', url }) 전송
 *   3) 열린 창이 없으면 clients.openWindow(url)
 * - 페이지 측(main-nav.js)은 위 메시지를 수신해 실제 라우팅을 수행한다.
 *
 * [알림센터(웹 UI) 미읽음 처리 개요]
 * - /notification 페이지 진입 시:
 *   1) /notification/list 로 목록 로딩
 *   2) 방금 본 ID들을 /notification/read 로 즉시 읽음 처리
 *   3) 하단 네비 종 아이콘의 빨간점은 /notification/unread-count 로 갱신
 *
 * 주: 이 파일은 SW 측 동작만 담당하며, 실제 리스트 로딩/읽음 처리/뱃지 갱신은
 *     페이지 JS(notification.js)가 수행한다.
 */


const CACHE_NAME = 'postHere-v2';
const FILES_TO_CACHE = ['/', '/css/main.css', '/js/main.js'];

self.addEventListener('install', (event) => {
    // 설치 단계: 앱 구동에 필요한 정적 리소스를 선 캐싱
    // - 오프라인/재방문 속도 개선
    // - 캐시에 추가 실패 시 install이 실패할 수 있으므로 event.waitUntil로 보장
    event.waitUntil(caches.open(CACHE_NAME).then((c) => c.addAll(FILES_TO_CACHE)));
    self.skipWaiting(); // 대기 상태 생략 → 즉시 이 SW 버전을 활성화 후보로 승격
});

self.addEventListener('activate', (event) => {
    // 활성화 단계: 이 SW가 모든 클라이언트를 즉시 제어
    // [확장 포인트] 구버전 캐시 삭제 로직 추가 가능: caches.keys → CACHE_NAME 필터링
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
    // 네트워크 요청 가로채기: Cache-First
    // - 정적 파일에 적합. 동적 API에는 stale-while-revalidate 등 분기 가능
    event.respondWith(caches.match(event.request).then((r) => r || fetch(event.request)));
});

// Push
self.addEventListener('push', (event) => {
    // 서버에서 Web Push로 전달된 메시지 수신
    // 기대 payload 형식(예시): {type, notificationId, actor{nickname,profilePhotoUrl}, text}
    // - title: actor.nickname이 있으면 사용, 없으면 기본값 'PostHere'
    // - body/icon: 텍스트/프로필 이미지 기반
    if (!event.data) return;
    const payload = event.data.json(); // {type, notificationId, actor{nickname,profilePhotoUrl}, text}
    const title = payload.actor?.nickname || 'PostHere';
    const body = payload.text || '';
    const icon = payload.actor?.profilePhotoUrl || '/icons/icon-192.png';

    // [확장 포인트]
    // - actions: [{action:'open', title:'열기'}] 등 버튼 추가 가능
    // - tag/renotify: 동일 알림 묶기/재알림 제어
    // - requireInteraction: 사용자가 닫기 전까지 유지(데스크톱 친화)
    // - data: 라우팅/추적 정보 확장
    event.waitUntil(
        self.registration.showNotification(title, {
            body, icon, badge: icon,
            data: {url: '/notification?focus=' + (payload.notificationId || '')}
        })
    );
});

// 클릭 → 앱 포커스 + 라우팅
// [알림 클릭 시 이동 처리 요약]
// - 여기서 postMessage({ type: 'NAVIGATE', url })를 창으로 보냄
// - 페이지(main-nav.js)는 'NAVIGATE' 메시지를 받아 라우팅(SPA/router 또는 location.*)을 수행
self.addEventListener('notificationclick', (event) => {
    // 알림 클릭 시:
    // 1) 알림 닫기
    // 2) 이미 열린 창이 있으면 focus + postMessage로 라우팅 지시
    // 3) 없으면 해당 URL로 새 창 오픈
    event.notification.close();
    const url = event.notification.data?.url || '/notification';
    event.waitUntil((async () => {
        const all = await clients.matchAll({type: 'window', includeUncontrolled: true});
        for (const c of all) {
            c.focus();
            // 프런트에서 'message' 이벤트(or navigator.serviceWorker.onmessage)로 수신하여
            // 클라이언트 라우터(NAVIGATE) 처리하면 됨
            c.postMessage({type: 'NAVIGATE', url});
            return;
        }
        await clients.openWindow(url);
    })());
});

/**
 * [참고/제안 - 변경 아님]
 * 1) 캐시 정리(activate):
 *    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
 * 2) 동적 컨텐츠 캐시:
 *    fetch 핸들러에서 URL/메서드/헤더에 따라 전략 분기(예: API는 네트워크 우선)
 * 3) push payload 안전성:
 *    try/catch로 JSON 파싱 및 필수 필드 검증, 이상 데이터 무시
 * 4) 알림 액션:
 *    actions를 추가하고 notificationclick에서 event.action 분기
 */
