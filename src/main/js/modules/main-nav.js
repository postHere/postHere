/**
 * main-nav.js
 *
 * 역할(정확한 명칭)
 * - Service Worker 등록 및 SW → 페이지 'NAVIGATE' 메시지 처리(알림 클릭 라우팅)
 * - VAPID 공개키(/push/vapid-public-key) 로드, 최초 사용자 제스처 시 권한 요청, Push 구독 생성
 * - 구독 정보 서버 저장(/push/subscribe, body: subscription.toJSON())
 *
 * 시퀀스(정확한 명칭)
 * 1) window.load → service-worker.js 등록
 * 2) /push/vapid-public-key 호출로 VAPID 공개키 로드
 * 3) (첫 사용자 클릭 시) Notification 권한 요청 → PushManager.subscribe() → /push/subscribe 저장
 * 4) SW 'notificationclick' → postMessage({type:'NAVIGATE', url}) 수신 → 실제 라우팅 수행
 *
 * 보안/운영 메모
 * - HTTPS 필수(로컬 localhost 예외), CSRF 메타태그 있으면 헤더 자동 첨부(authHeaders)
 * - 구독 저장은 서버에서 upsert(중복 허용) 형태 권장
 * - 메시지 수신 시 잘못된/외부 URL 방지(새 URL을 origin 기준으로 정규화)
 */

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

/*
    여기있던 코드 modules 디렉토리로 분배됨
 */

import {authHeaders} from './utils.js';

export function initMainNav() {

    // <!-- 진입 시 미읽음 개수 반영: >0 이면 빨간 점 표시
    //  - 이후 주기 폴링(15s)은 /js/notification.js에서만 수행(중복 폴링 방지) -->
    async function updateUnreadCount() {
        try {
            const res = await fetch('/notification/unread-count', {
                method: 'POST',
                // main-nav.js의 authHeaders가 있으면 CSRF 자동 첨부, 없으면 무시
                headers: (typeof authHeaders === 'function')
                    ? authHeaders({Accept: 'application/json'})
                    : {Accept: 'application/json'}
            });
            if (!res.ok) return;
            const n = await res.json(); // 서버가 숫자(long)로 응답
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = (n > 0) ? 'inline-block' : 'none';
        } catch (e) {
            // 네트워크 실패 시 조용히 패스(네비 사용성 유지)
            console.error('Failed to update unread notification count:', e);
        }
    }

    updateUnreadCount();
}
