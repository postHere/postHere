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
