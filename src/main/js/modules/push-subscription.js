// src/main/js/push-subscription.js
import {authHeaders, urlBase64ToUint8Array} from "./utils";

window.__pushSubModuleLoaded = true; // [추가] 모듈 로드 확인용 전역 플래그

export function initPushSubscribe() {
    console.log('[push] initPushSubscribe START'); // [추가] 함수 진입 확인용 로그

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

    // 구독을 서버에 저장
    async function saveSubscription(sub) {
        const body = sub.toJSON(); // {endpoint, keys:{p256dh, auth}}
        const res = await fetch('/push/subscribe', {
            method: 'POST',
            headers: authHeaders({'Content-Type': 'application/json', Accept: 'application/json'}),
            body: JSON.stringify(body),
            credentials: 'include', // [추가] 세션 쿠키 포함 (401 방지)
        });
        if (!res.ok) throw new Error('/push/subscribe HTTP ' + res.status);
        console.log('[push] subscription saved to server (200)');
    }

    // 실제 구독 시퀀스
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

        const reg = await navigator.serviceWorker.ready;
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

        await saveSubscription(sub);

        // SW → 페이지 네비 메시지
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

            if (Notification.permission === 'granted') {
                ensurePushSubscriptionWithGesture(publicKey).catch(console.error);
            } else {
                attachOnceForGesture(publicKey);
            }
        } catch (e) {
            console.error('[push] cannot load VAPID public key:', e);
        }
    }

    // 디버그용 전역 함수
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

    // [추가] init 시도 시작
    fetchVapidKeyAndStart();
}
