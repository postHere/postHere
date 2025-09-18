/**
 * main-nav.js
 * - 네비 활성화 유지
 * - 미읽음 배지 갱신(엔드포인트 자동 폴백)
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

    // ===== 엔드포인트 폴백 유틸 =====
    const API_BASES = ['/api/notifications', '/notification']; // 둘 중 되는 걸 자동 사용
    async function fetchJsonFallback(path, init = {}) {
        const headers = (typeof authHeaders === 'function')
            ? authHeaders({Accept: 'application/json', ...(init.headers || {})})
            : {Accept: 'application/json', ...(init.headers || {})};

        for (const base of API_BASES) {
            const res = await fetch(`${base}${path}`, {
                credentials: 'include',
                ...init,
                headers
            });
            const ct = res.headers.get('content-type') || '';
            if (res.ok && ct.includes('application/json')) {
                return res.json(); // number든 object든 JSON으로 파싱
            }
            // 401이면 추가 시도 의미 없음
            if (res.status === 401) throw new Error('UNAUTHORIZED');
            // HTML 등 응답(<!DOCTYPE …)은 다음 베이스로 폴백
        }
        throw new Error('All endpoints failed for ' + path);
    }

    // ===== 미읽음 배지 갱신 =====
    async function refreshUnreadBadge() {
        try {
            const count = await fetchJsonFallback('/unread-count', {method: 'POST'});
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = (Number(count) > 0) ? 'inline-block' : 'none';
        } catch (e) {
            // HTML 응답으로 인한 JSON 파싱 에러(Unexpected token '<') 방지됨
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
