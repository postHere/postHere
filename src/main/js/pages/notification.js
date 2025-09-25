// /static/js/pages/notification.js
// 역할: 알림 목록 로딩 + (읽음 처리 단일 엔드포인트 연계)
// 템플릿과의 매칭: #list

export function initNotification() {
    // 이 페이지가 아니면 종료
    if (document.body?.id !== 'page-notifications') return;

    let readAllOnce = false;

    const $list = document.querySelector('#list');

    if (!$list) {
        console.warn('[notification] 알림 리스트 요소를 찾지 못했습니다.');
        return;
    }

    function syncFooterHeightVar() {
        try {
            const nav = document.querySelector('.main-nav-bar');
            const h = nav ? Math.ceil(nav.getBoundingClientRect().height) : 0;
            const px = (h && Number.isFinite(h)) ? `${h}px` : null;
            if (px) document.documentElement.style.setProperty('--footer-height', px);
        } catch { /* ignore */
        }
    }

    syncFooterHeightVar();
    setTimeout(syncFooterHeightVar, 300);
    window.addEventListener('resize', syncFooterHeightVar);
    window.addEventListener('orientationchange', syncFooterHeightVar);

    const API_BASE = '/api/notifications';

    async function postJson(url, bodyObj) {
        const res = await fetch(url, {
            method: 'POST',
            credentials: 'include',
            headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
            body: bodyObj ? JSON.stringify(bodyObj) : '{}'
        });
        const ct = res.headers.get('content-type') || '';
        if (!res.ok || !ct.includes('application/json')) {
            if (res.status === 401) throw new Error('UNAUTHORIZED');
            throw new Error(`Unexpected response: ${res.status}`);
        }
        return res.json();
    }

    function timeAgo(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        if (isNaN(d.getTime())) return '';
        const diff = Date.now() - d.getTime();
        const s = Math.floor(diff / 1000);
        if (s < 60) return 'now';
        const m = Math.floor(s / 60);
        if (m < 60) return `${m}m`;
        const h = Math.floor(m / 60);
        if (h < 24) return `${h}h`;
        const dd = Math.floor(h / 24);
        return `${dd}d`;
    }

    function setNavDotVisible(visible) {
        const dot = document.getElementById('nav-bell-dot');
        if (dot) dot.style.display = visible ? 'inline-block' : 'none';
    }

    function attachLeaveHandlersOnce() {
        const hide = () => setNavDotVisible(false);
        window.addEventListener('pagehide', hide, {once: true});
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) hide();
        }, {once: true});
        window.addEventListener('beforeunload', hide, {once: true});
    }

    function render(items = []) {
        $list.innerHTML = '';
        if (!items.length) {
            const empty = document.createElement('div');
            empty.className = 'empty';
            empty.textContent = '새 알림이 없습니다.';
            $list.appendChild(empty);
            return;
        }

        const frag = document.createDocumentFragment();

        items.forEach((it) => {
            const id = it.id ?? it.notificationId ?? it.notification_pk ?? null;
            const code = it.type ?? it.notificationCode ?? it.code ?? 'NOTI';
            const text = it.text ?? it.message ?? (code === 'FOLLOW' ? 'Started following you' : code);
            const actorNick = it.actor?.nickname ?? it.followerNickname ?? it.actor?.name ?? it.follower?.nickname ?? it.following?.follower?.nickname ?? '';
            const avatarUrl = it.actor?.profilePhotoUrl ?? it.followerProfilePhotoUrl ?? '/images/profile-default.png';
            const createdAt = it.createdAt ?? it.created_at ?? null;
            const read = (typeof it.read === 'boolean') ? it.read : (typeof it.checkStatus === 'boolean') ? it.checkStatus : (typeof it.checked === 'boolean') ? it.checked : false;

            const row = document.createElement('a');
            row.className = 'noti-card';
            if (id != null) row.dataset.id = String(id);
            row.href = actorNick ? `/profile/${encodeURIComponent(actorNick)}` : '#';
            row.setAttribute('aria-label', actorNick ? `${actorNick} ${text}` : text);
            row.dataset.unread = String(!read);

            const img = document.createElement('img');
            img.className = 'noti-avatar';
            img.alt = actorNick ? `${actorNick} profile` : 'profile';
            img.src = avatarUrl || '/images/profile-default.png';
            img.onerror = () => {
                img.src = '/images/profile-default.png';
            };
            row.appendChild(img);

            const main = document.createElement('div');
            main.className = 'noti-main';
            const head = document.createElement('div');
            head.className = 'noti-head';
            const nickEl = document.createElement('span');
            nickEl.className = 'noti-nick';
            nickEl.textContent = actorNick ? `@${actorNick}` : '(알 수 없음)';
            const timeEl = document.createElement('span');
            timeEl.className = 'noti-time';
            timeEl.textContent = timeAgo(createdAt);
            head.appendChild(nickEl);
            head.appendChild(timeEl);
            const textEl = document.createElement('div');
            textEl.className = 'noti-text';
            textEl.textContent = String(text);
            main.appendChild(head);
            main.appendChild(textEl);
            row.appendChild(main);
            frag.appendChild(row);
        });
        $list.appendChild(frag);
    }

    async function load() {
        try {
            const data = await postJson(`${API_BASE}/list`, {page: 0, size: 100});
            const items = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : [];

            render(items);

            if (!readAllOnce) {
                readAllOnce = true;
                try {
                    await postJson('/notification', {});
                    attachLeaveHandlersOnce();
                } catch (e) {
                    console.debug('[notification] read-all failed:', e?.message || e);
                }
            }
        } catch (e) {
            console.error('[notification] load failed:', e?.message || e);
            render([]);
        }
    }

    // 최초 로드
    load();
}