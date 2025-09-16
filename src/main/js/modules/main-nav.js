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
    const navLinks = document.querySelectorAll('.footer-nav a');
    const currentPath = window.location.pathname;

    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        link.classList.remove('active'); // 모든 active 클래스 초기화

        // 1. 메인 페이지(/forumMain)는 정확히 해당 경로일 때만 활성화
        if (href === '/forumMain' && currentPath === '/forumMain') {
            link.classList.add('active');
        }
            // 2. 글쓰기 페이지(/forum)는 '/forum'으로 시작하는 모든 하위 경로에서 활성화
        //    단, '/forumMain'과 같은 메인 페이지는 제외
        else if (href === '/forum' && currentPath.startsWith('/forum')) {
            // '/forumMain' 페이지는 제외하고, '/forum', '/forum/write', '/forum/edit' 등에서 활성화
            if (currentPath !== '/forumMain') {
                link.classList.add('active');
            }
        }
        // 3. 그 외의 링크는 현재 경로가 href로 시작할 때 활성화
        else if (href !== '/forumMain' && href !== '/forum' && currentPath.startsWith(href)) {
            link.classList.add('active');
        }
    });

    // 어떤 링크도 활성화되지 않았을 경우, 기본적으로 홈 버튼을 활성화
    const activeLink = document.querySelector('.footer-nav a.active');
    if (!activeLink) {
        const homeLink = document.querySelector('.footer-nav a[href="/forumMain"]');
        if (homeLink) {
            homeLink.classList.add('active');
        }
    }

    // 클릭 이벤트 리스너 추가
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            navLinks.forEach(item => item.classList.remove('active'));
            e.currentTarget.classList.add('active');
        });
    });

    // 알림 미읽음 카운트 업데이트 로직은 그대로 유지
    async function updateUnreadCount() {
        try {
            const res = await fetch('/notification/unread-count', {
                method: 'POST',
                headers: (typeof authHeaders === 'function')
                    ? authHeaders({Accept: 'application/json'})
                    : {Accept: 'application/json'}
            });
            if (!res.ok) return;
            const n = await res.json();
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = (n > 0) ? 'inline-block' : 'none';
        } catch (e) {
            console.error('Failed to update unread notification count:', e);
        }
    }

    updateUnreadCount();
}