// src/main/js/pwa.js
import {initPushSubscribe} from "./push-subscription";


// === 페이지 로드 시 실행 ===
// (기존 코드 유지: load 시 /service-worker.js 등록 + 이후 공개키 fetch 시작)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', async () => {

        window.__pwaLoaded = true; // [추가] 디버그용 전역 플래그
        console.log('[pwa] pwa.js LOADED');

        try {
            // ✅ 기존 기능: 로드 시 SW 등록 (동일 경로/스코프)
            const reg = await navigator.serviceWorker.register('/service-worker.js');
            console.log('[push] SW registered:', reg.scope);

            // 푸시 알림 구독
            // [설명] initPushSubscribe 내부에서 fetchVapidKeyAndStart()가 실행됨.
            //        따라서 여기서는 따로 fetchVapidKeyAndStart()를 호출할 필요 없음.
            initPushSubscribe();

            // 서비스 워커로부터 오는 메시지(알림 클릭 등) 처리
            navigator.serviceWorker.addEventListener('message', (e) => {
                if (e.data?.type === 'NAVIGATE') {
                    const url = new URL(e.data.url, location.origin).toString();
                    if (location.href !== url) location.href = url;
                }
            });
        } catch (e) {
            console.error('[push] SW register failed', e);
        }
    });
}
