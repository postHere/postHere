/**
 * main-nav.js
 * - 네비 활성화 유지
 * - 미읽음 배지 갱신(단일 엔드포인트)
 * - SW NAVIGATE 메시지(있으면) 처리
 */
import {authHeaders} from './utils.js';

export function initMainNav() {
    const navLinks = document.querySelectorAll('.footer-nav a');
    const currentPath = window.location.pathname;

    // ===== 기존 활성화 로직 유지 =====
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        link.classList.remove('active');

        if (href === '/forumMain' && currentPath === '/forumMain') {
            link.classList.add('active');
        } else if (href === '/forum' && currentPath.startsWith('/forum') && currentPath !== '/forumMain') {
            link.classList.add('active');
        } else if (href !== '/forumMain' && href !== '/forum' && currentPath.startsWith(href)) {
            link.classList.add('active');
        }
    });

    // [추가] Friends 페이지에서는 '사람(프로필)' 아이콘을 활성화로 강제 지정
    //        - 기존 규칙으로는 /friends 가 /profile 과 매칭되지 않으므로 보정
    if (currentPath.startsWith('/friends')) {
        navLinks.forEach(item => item.classList.remove('active'));
        const profileLink = document.querySelector('.footer-nav a[href="/profile"]');
        if (profileLink) profileLink.classList.add('active');
    }

    if (!document.querySelector('.footer-nav a.active')) {
        const homeLink = document.querySelector('.footer-nav a[href="/forumMain"]');
        if (homeLink) homeLink.classList.add('active');
    }

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            navLinks.forEach(item => item.classList.remove('active'));
            e.currentTarget.classList.add('active');
        });
    });

    // ===== 1. 사각형 토글 기능 =====
    const toggleButton = document.getElementById('toggle-button');
    const squareToggle = document.getElementById('square-toggle');

    // 토글 열기/닫기 함수
    function toggleSquare(open) {
        if (!squareToggle) return; // ▼▼▼ [수정됨] null 체크

        if (open) {
            squareToggle.classList.add('active');
        } else {
            squareToggle.classList.remove('active');
        }
    }

    // 플러스 버튼 클릭 시 토글
    if (toggleButton) {
        toggleButton.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();

            if (!squareToggle) {
                return;
            }

            const isOpen = squareToggle.classList.contains('active');
            toggleSquare(!isOpen);
        });
    }

    // 토글 항목 클릭 시 해당 경로로 이동 (클릭 이벤트 추가)
    const toggleItems = document.querySelectorAll('.toggle-item'); // ▼▼▼ [수정됨] toggle-item 참조
    toggleItems.forEach(item => {
        item.addEventListener('click', (e) => {
            // 클릭한 항목의 href를 가져와 페이지 이동
            const href = item.getAttribute('href');
            if (href) {
                location.href = href;
                // 토글 닫기
                toggleSquare(false);
            }
        });
    });


    // ===== 미읽음 배지 갱신 (단일 호출) =====
    async function fetchUnreadCount() {
        const res = await fetch('/api/notifications/unread-count', {
            method: 'POST',
            credentials: 'include',
            headers: authHeaders({Accept: 'application/json'})
        });
        const ct = res.headers.get('content-type') || '';
        if (!res.ok || !ct.includes('application/json')) {
            if (res.status === 401) throw new Error('UNAUTHORIZED');
            throw new Error('Unexpected response');
        }
        return res.json(); // number
    }

    async function refreshUnreadBadge() {
        try {
            const count = await fetchUnreadCount();
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = (Number(count) > 0) ? 'inline-block' : 'none';
        } catch (e) {
            // 인증 만료/오류 시 조용히 실패 + 도트 숨김
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = 'none';
            console.error('Failed to update unread count:', e?.message || e);
        }
    }

    refreshUnreadBadge();
    window.addEventListener('focus', refreshUnreadBadge);
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') refreshUnreadBadge();
    });

    // ===== (옵션) SW → NAVIGATE 처리 =====
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.addEventListener('message', (evt) => {
            const msg = evt.data;
            if (!msg || msg.type !== 'NAVIGATE' || !msg.url) return;
            try {
                const url = new URL(msg.url, location.origin);
                if (url.origin === location.origin) location.assign(url.href);
            } catch {
                // ignore
            }
        });
    }
}
