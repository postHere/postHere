// /static/js/pages/notification.js
// 역할: 알림 목록 로딩 + 페이징 + (있다면) 미읽음 배지 갱신
// 템플릿과의 매칭: #list, #prev, #next, #page

export function initNotification() {
    // 이 페이지가 아니면 종료
    if (document.body?.id !== 'page-notifications') return;

    const $list = document.querySelector('#list');
    const $btnPrev = document.querySelector('#prev');
    const $btnNext = document.querySelector('#next');
    const $page = document.querySelector('#page');

    if (!$list || !$btnPrev || !$btnNext || !$page) {
        console.warn('[notification] 필수 DOM 요소를 찾지 못했습니다.');
        return;
    }

    // 백엔드 엔드포인트 (우선 /api, 필요 시 /notification 폴백)
    const API_BASES = ['/api/notifications', '/notification'];

    async function fetchJsonFallback(path, init = {}) {
        for (const base of API_BASES) {
            const res = await fetch(`${base}${path}`, {
                credentials: 'include',
                headers: {Accept: 'application/json', ...(init.headers || {})},
                ...init,
            });
            const ct = res.headers.get('content-type') || '';
            if (res.ok && ct.includes('application/json')) return res.json();
            if (res.status === 401) throw new Error('UNAUTHORIZED');
            // HTML이면 다음 베이스 시도
        }
        throw new Error('All endpoints failed: ' + path);
    }

    // 페이지 상태
    let page = 0;
    const size = 20;
    let last = false;

    const fmt = (ts) => {
        if (!ts) return '';
        const d = new Date(ts);
        return isNaN(d.getTime()) ? String(ts) : d.toLocaleString();
    };

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

            // ✅ 응답 필드명 호환
            const code = it.type ?? it.notificationCode ?? it.code ?? 'NOTI';
            const text =
                it.text ??
                it.message ??
                (code === 'FOLLOW' ? 'Started following you' : code);

            // ✅ 닉네임 우선 표시(여러 케이스 대응)
            const actorNick =
                it.actor?.nickname ??
                it.followerNickname ??
                it.actor?.name ??
                it.follower?.nickname ??
                it.following?.follower?.nickname ??
                '';

            const createdAt = it.createdAt ?? it.created_at ?? null;

            const row = document.createElement('div');
            row.className = 'noti-card';
            if (id != null) row.dataset.id = String(id);

            const title = document.createElement('div');
            title.className = 'noti-title';

            // @nickname + 코드(FOLLOW) 같이 보여주기
            if (actorNick) {
                const nickEl = document.createElement('span');
                nickEl.className = 'noti-actor';
                nickEl.textContent = `@${actorNick}`;
                title.appendChild(nickEl);
                title.appendChild(document.createTextNode(' '));
            }
            if (code) {
                const codeEl = document.createElement('span');
                codeEl.className = 'noti-code';
                codeEl.textContent = code;
                title.appendChild(codeEl);
            }

            const body = document.createElement('div');
            body.className = 'noti-text';
            body.textContent = String(text);

            const time = document.createElement('div');
            time.className = 'noti-date';
            time.textContent = fmt(createdAt);

            row.appendChild(title);
            row.appendChild(body);
            row.appendChild(time);
            frag.appendChild(row);
        });

        $list.appendChild(frag);
    }

    async function refreshUnreadBadge() {
        try {
            const count = await fetchJsonFallback('/unread-count', {method: 'POST'});
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = Number(count) > 0 ? 'inline-block' : 'none';
        } catch (e) {
            console.debug('[notification] unread badge update skipped:', e?.message || e);
        }
    }

    async function load(p = 0) {
        try {
            const data = await fetchJsonFallback('/list', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({page: p, size}),
            });

            // {items, unreadCount} 형태 또는 배열 대응
            const items = Array.isArray(data?.items)
                ? data.items
                : Array.isArray(data)
                    ? data
                    : [];
            render(items);

            // 마지막 페이지 추정
            last = items.length < size;

            // 페이지 표시/버튼 상태
            page = p;
            $page.textContent = String(page + 1);
            $btnPrev.disabled = page <= 0;
            $btnPrev.setAttribute('aria-disabled', String($btnPrev.disabled));
            $btnNext.disabled = !!last;
            $btnNext.setAttribute('aria-disabled', String($btnNext.disabled));

            // 미읽음 배지 업데이트
            if (typeof data?.unreadCount === 'number') {
                const dot = document.getElementById('nav-bell-dot');
                if (dot) dot.style.display = data.unreadCount > 0 ? 'inline-block' : 'none';
            } else {
                refreshUnreadBadge();
            }
        } catch (e) {
            console.error('[notification] load failed:', e?.message || e);
        }
    }

    // 이벤트
    $btnPrev.addEventListener('click', () => {
        if (page > 0) load(page - 1);
    });
    $btnNext.addEventListener('click', () => {
        if (!last) load(page + 1);
    });

    // 최초 로드
    load(0);
}
