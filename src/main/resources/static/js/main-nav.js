// === /static/js/main-nav.js ===
// 기존 "load 시 service-worker.js 등록" 흐름을 유지하면서,
// 공개키 fetch → (첫 사용자 클릭 시) 권한요청 → 구독 생성 → 서버 저장을 추가했습니다.

/**
 * [SW → Page 'NAVIGATE' 메시지 처리 책임]
 * - SW(notificationclick)가 보낸 postMessage({ type: 'NAVIGATE', url })를 수신해
 *   실제 화면 전환을 수행한다.
 * - 라우팅 방식 선택:
 *   A) SPA 라우터: router.push(url) 또는 history.pushState 후 렌더
 *   B) 단순 이동: location.assign(url) (새로고침 발생)
 * - 안정성:
 *   - 잘못된 url/null 방어
 *   - 필요 시 event.origin 검증
 */


// CSRF 헬퍼(스프링 보안 사용 시 메타태그가 있으면 자동 첨부; 없으면 무시)
function authHeaders(base = {}) {
    const headers = {...base};
    const token = document.querySelector('meta[name="_csrf"]')?.content || '';
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    if (token) headers[headerName] = token;
    return headers;
}

// base64url → Uint8Array (VAPID 공개키 변환용)
function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const output = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) output[i] = rawData.charCodeAt(i);
    return output;
}

// 권한 확보
async function ensurePermission() {
    if (!('Notification' in window)) {
        console.warn('[push] Notification API 미지원');
        return false;
    }
    if (Notification.permission === 'granted') return true;
    if (Notification.permission === 'denied') {
        console.warn('[push] 알림이 차단되어 있습니다. 주소창 ▶ 사이트 설정 ▶ 알림 ▶ 허용으로 변경하세요.');
        return false;
    }
    const p = await Notification.requestPermission();
    return p === 'granted';
}

// 구독을 서버에 저장 (서버 DTO: { endpoint, keys:{p256dh, auth} } 형식과 일치하도록 toJSON 그대로 전송)
async function saveSubscription(sub) {
    const body = sub.toJSON(); // {endpoint, keys:{p256dh, auth}}
    const res = await fetch('/push/subscribe', {
        method: 'POST',
        headers: authHeaders({'Content-Type': 'application/json', Accept: 'application/json'}),
        body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error('/push/subscribe HTTP ' + res.status);
    console.log('[push] subscription saved to server (200)');
}

// 실제 구독 시퀀스 (브라우저 제스처 이후 실행)
async function ensurePushSubscriptionWithGesture(vapidPublicKey) {
    if (!('serviceWorker' in navigator)) {
        console.warn('[push] serviceWorker 미지원');
        return;
    }
    if (!('PushManager' in window)) {
        console.warn('[push] PushManager 미지원');
        return;
    }
    if (!window.isSecureContext && location.hostname !== 'localhost') {
        console.warn('[push] 보안 컨텍스트(HTTPS) 필요');
        return;
    }

    // 기존 코드와 동일하게 루트(/) scope로 등록된 SW 사용
    const reg = await navigator.serviceWorker.ready; // register 이후 ready 대기
    console.log('[push] SW ready. scope=', reg.scope);

    let sub = await reg.pushManager.getSubscription();
    if (!sub) {
        console.log('[push] creating new subscription…');
        sub = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
        });
        console.log('[push] subscribe() done');
    } else {
        console.log('[push] existing subscription found');
    }

    // 서버에 저장(신규/기존 모두 upsert용으로 전송)
    await saveSubscription(sub);

    // SW → 페이지 네비 메시지(푸시 클릭 시 이동)
    // [SW 메시지 수신부]
    // navigator.serviceWorker.addEventListener('message', (e) => {
    //   const { type, url } = e.data || {};
    //   if (type === 'NAVIGATE' && url) {
    //     // SPA 라우팅 예: router.push(url) 또는 history.pushState(...)
    //     // 단순 이동 예: location.assign(url)
    //   }
    // });

    navigator.serviceWorker.addEventListener('message', (e) => {
        if (e.data?.type === 'NAVIGATE') {
            const url = new URL(e.data.url, location.origin).toString();
            if (location.href !== url) location.href = url;
        }
    });
}

// 최초 1회 사용자 제스처(클릭)에만 권한/구독 시도
function attachOnceForGesture(vapidPublicKey) {
    if (Notification.permission === 'granted') {
        // 이미 허용이면 즉시 진행
        ensurePushSubscriptionWithGesture(vapidPublicKey).catch(console.error);
        return;
    }
    const once = async () => {
        document.removeEventListener('click', once, true);
        const ok = await ensurePermission();
        if (ok) await ensurePushSubscriptionWithGesture(vapidPublicKey);
    };
    document.addEventListener('click', once, true);
    console.log('[push] waiting first user click to request permission…');
}

// 공개키 받아오고 클릭 훅 장착
async function fetchVapidKeyAndStart() {
    try {
        const res = await fetch('/push/vapid-public-key', {headers: authHeaders({Accept: 'application/json'})});
        if (!res.ok) {
            console.warn('[push] Failed to load VAPID key:', res.status);
            return;
        }
        const {publicKey} = await res.json();
        if (!publicKey) {
            console.warn('[push] Empty VAPID public key');
            return;
        }
        console.log('[push] VAPID key loaded');
        attachOnceForGesture(publicKey);
    } catch (e) {
        console.error('[push] cannot load VAPID public key:', e);
    }
}

// 디버그용 전역 함수(콘솔 수동 트리거)
window.__forceSubscribe = async () => {
    try {
        const res = await fetch('/push/vapid-public-key', {headers: authHeaders({Accept: 'application/json'})});
        const {publicKey} = await res.json();
        const ok = await ensurePermission();
        if (ok) await ensurePushSubscriptionWithGesture(publicKey);
    } catch (e) {
        console.error(e);
    }
};
window.__unsubscribe = async () => {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    if (sub) {
        await sub.unsubscribe();
        console.log('[push] unsubscribed.');
    } else {
        console.log('[push] no existing subscription.');
    }
};

// === 페이지 로드 시 실행 ===
// (기존 코드 유지: load 시 /service-worker.js 등록 + 이후 공개키 fetch 시작)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', async () => {
        try {
            // ✅ 기존 기능: 로드 시 SW 등록 (동일 경로/스코프)
            const reg = await navigator.serviceWorker.register('/service-worker.js');
            console.log('[push] SW registered:', reg.scope);

            // 공개키를 받아온 뒤, 첫 사용자 클릭에서 권한/구독을 시도
            await fetchVapidKeyAndStart();
        } catch (e) {
            console.error('[push] SW register failed', e);
        }
    });
}
