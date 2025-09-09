// if ('serviceWorker' in navigator) {
//     window.addEventListener('load', () => {
//         navigator.serviceWorker.register('/service-worker.js');
//     });
// }
// GPT가 밑에 껄로 확장해줬는데 맘에 안들면 기존껄로 유지하죠

// === 페이지 로드 시 실행 ===
// (기존 파일에 있던 "load 시 SW 등록" 흐름을 포함하면서 확장)
// if ('serviceWorker' in navigator) {
//     window.addEventListener('load', bootstrapPush);
// }
//
//
// // === 유틸 ===
// function urlBase64ToUint8Array(base64String) {
//     const padding = '='.repeat((4 - base64String.length % 4) % 4);
//     const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
//     const raw = atob(base64);
//     const output = new Uint8Array(raw.length);
//     for (let i = 0; i < raw.length; ++i) output[i] = raw.charCodeAt(i);
//     return output;
// }
//
// function authHeaders(base = {}) {
//     const headers = {...base};
//     const token = document.querySelector('meta[name="_csrf"]')?.content || '';
//     const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
//     if (token) headers[headerName] = token;
//     return headers;
// }
//
// // SW → 페이지 메시지 중복 등록 방지용
// let __pushMsgBound = false;
//
// // === 핵심 ===
// async function getOrRegisterServiceWorker() {
//     // 기존에 있으면 재사용, 없으면 등록
//     let reg = await navigator.serviceWorker.getRegistration('/service-worker.js');
//     if (!reg) reg = await navigator.serviceWorker.register('/service-worker.js');
//     return reg;
// }
//
// async function ensurePushSubscription(vapidPublicKey, reg) {
//     if (!('PushManager' in window)) {
//         console.debug('[push] PushManager unsupported');
//         return;
//     }
//
//     // 알림 권한
//     const perm = await Notification.requestPermission();
//     if (perm !== 'granted') {
//         console.debug('[push] Permission not granted:', perm);
//         return;
//     }
//
//     // 구독 조회/생성
//     let sub = await reg.pushManager.getSubscription();
//     if (!sub) {
//         sub = await reg.pushManager.subscribe({
//             userVisibleOnly: true,
//             applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
//         });
//     }
//
//     // 서버에 Body Only로 구독 저장 (endpoint/p256dh/auth)
//     const payload = sub.toJSON ? sub.toJSON() : {endpoint: sub.endpoint, keys: sub.keys};
//     await fetch('/push/subscribe', {
//         method: 'POST',
//         headers: authHeaders({'Content-Type': 'application/json'}),
//         body: JSON.stringify(payload),
//     });
//
//     // SW → 페이지 네비 메시지 바인딩(푸시 클릭 시 /notification 등으로 이동)
//     if (!__pushMsgBound) {
//         navigator.serviceWorker.addEventListener('message', (e) => {
//             if (e.data?.type === 'NAVIGATE') {
//                 const url = new URL(e.data.url, location.origin).toString();
//                 if (location.href !== url) location.href = url;
//             }
//         });
//         __pushMsgBound = true;
//     }
// }
//
// async function bootstrapPush() {
//     try {
//         const reg = await getOrRegisterServiceWorker(); // ✅ 기존 코드 흐름 유지(로드 시 등록)
//
//         // 서버에서 공개 VAPID 키 받아오기
//         const res = await fetch('/push/vapid-public-key');
//         if (!res.ok) {
//             console.warn('[push] Failed to load VAPID key:', res.status);
//             return;
//         }
//         const data = await res.json();
//         const publicKey = data?.publicKey;
//         if (!publicKey) {
//             console.warn('[push] Empty VAPID key');
//             return;
//         }
//
//         await ensurePushSubscription(publicKey, reg);
//     } catch (err) {
//         console.warn('[push] init failed:', err);
//     }
// }
//
// // 첫 사용자 클릭에서 권한/구독 시도 (브라우저 제스처 요구 대응)
// (function attachOnceForGesture() {
//     // 이미 허용이면 즉시 진행
//     if (Notification.permission === 'granted') {
//         ensurePushSubscriptionWithGesture();
//         return;
//     }
//     // 한 번 클릭 시도만 훅
//     const once = async () => {
//         document.removeEventListener('click', once, true);
//         const ok = await ensurePermission();
//         if (ok) await ensurePushSubscriptionWithGesture();
//     };
//     document.addEventListener('click', once, true);
// })();

// === /static/js/main-nav.js (전체 교체) ===

// CSRF 헬퍼(스프링 보안 사용 시)
function authHeaders(base = {}) {
    const headers = {...base};
    const token = document.querySelector('meta[name="_csrf"]')?.content || '';
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    if (token) headers[headerName] = token;
    return headers;
}

let VAPID_PUBLIC_KEY = '';

// 공개키 받아오면 구독 시퀀스 시작
async function fetchVapidKeyAndStart() {
    try {
        const res = await fetch('/push/vapid-public-key', {headers: authHeaders({Accept: 'application/json'})});
        if (!res.ok) throw new Error('vapid-public-key HTTP ' + res.status);
        const data = await res.json();
        VAPID_PUBLIC_KEY = data.publicKey;
        console.log('[push] VAPID key loaded');
        // 키를 받은 뒤에만 구독 시도
        attachOnceForGesture();
    } catch (e) {
        console.error('[push] cannot load VAPID public key:', e);
    }
}

// 권한 요청
async function ensurePermission() {
    console.log('[push] Notification.permission =', Notification.permission);
    if (Notification.permission === 'granted') return true;
    if (Notification.permission === 'denied') {
        console.warn('[push] permission denied: 브라우저 주소창 ▶ 사이트 설정 ▶ 알림 ▶ 허용으로 변경 필요');
        return false;
    }
    const p = await Notification.requestPermission();
    console.log('[push] requestPermission() =>', p);
    return p === 'granted';
}

// ArrayBuffer 변환
function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const output = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) output[i] = rawData.charCodeAt(i);
    return output;
}

// 구독을 서버에 항상 저장(신규/기존 모두)
async function saveSubscription(sub) {
    const body = {
        endpoint: sub.endpoint,
        p256dh: btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('p256dh')))),
        auth: btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('auth')))),
    };
    console.log('[push] saving subscription to server…', body.endpoint);
    const res = await fetch('/push/subscribe', {
        method: 'POST',
        headers: authHeaders({'Content-Type': 'application/json', Accept: 'application/json'}),
        body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error('/push/subscribe HTTP ' + res.status);
    console.log('[push] saved to server (200).');
}

// 실제 구독 시퀀스
async function ensurePushSubscriptionWithGesture() {
    if (!('serviceWorker' in navigator)) {
        console.warn('[push] no serviceWorker support');
        return;
    }
    if (!window.isSecureContext && location.hostname !== 'localhost') {
        console.warn('[push] secure context(https) 필요');
        return;
    }
    // SW 준비 대기
    const reg = await navigator.serviceWorker.ready;
    console.log('[push] SW ready. scope =', reg.scope);

    let sub = await reg.pushManager.getSubscription();
    console.log('[push] existing subscription =', !!sub);

    if (!sub) {
        console.log('[push] creating new subscription…');
        sub = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
        });
        console.log('[push] subscribe() done');
    }
    // 신규든 기존이든 항상 서버에 저장 시도(서버는 upsert/unique 처리)
    await saveSubscription(sub);
}

// 사용자 첫 클릭에 한 번만 권한/구독 시도(브라우저 제스처 정책 대응)
function attachOnceForGesture() {
    if (Notification.permission === 'granted') {
        // 이미 허용이면 즉시
        ensurePushSubscriptionWithGesture().catch(console.error);
        return;
    }
    const once = async () => {
        document.removeEventListener('click', once, true);
        const ok = await ensurePermission();
        if (ok) await ensurePushSubscriptionWithGesture();
    };
    document.addEventListener('click', once, true);
    console.log('[push] waiting first user click to request permission…');
}

// 디버그용 전역 함수(콘솔에서 수동 트리거)
window.__forceSubscribe = async () => {
    const ok = await ensurePermission();
    if (ok) await ensurePushSubscriptionWithGesture();
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

// SW 등록 + 공개키 가져오기 시작
if ('serviceWorker' in navigator) {
    window.addEventListener('load', async () => {
        try {
            const reg = await navigator.serviceWorker.register('/service-worker.js');
            console.log('[push] SW registered:', reg.scope);
            // 공개키 받아오면 attachOnceForGesture()가 내부에서 실행됨
            await fetchVapidKeyAndStart();
        } catch (e) {
            console.error('[push] SW register failed', e);
        }
    });
}
