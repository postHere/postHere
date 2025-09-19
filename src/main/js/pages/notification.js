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
    const size = 10; // ✅ 10개 페이징
    let last = false;
    let readAllOnce = false; // ✅ 진입 후 1회만 read-all

    // createdAt → "now" | "Xm" | "Xh" | "Xd"
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
        return `${dd}d`; // ✅ 7일 이상도 Xd 유지
    }

    // (기존) 미읽음 배지 갱신 유틸 — 본 페이지 체류 중에는 자동으로 숨기지 않기 위해 사용 안 함
    async function refreshUnreadBadge() {
        try {
            const count = await fetchJsonFallback('/unread-count', {method: 'POST'});
            const dot = document.getElementById('nav-bell-dot');
            if (dot) dot.style.display = Number(count) > 0 ? 'inline-block' : 'none';
        } catch (e) {
            console.debug('[notification] unread badge update skipped:', e?.message || e);
        }
    }

    // 전역 도트 표시/숨김
    function setNavDotVisible(visible) {
        const dot = document.getElementById('nav-bell-dot');
        if (dot) dot.style.display = visible ? 'inline-block' : 'none';
    }

    // 페이지를 떠날 때(탭 이동/닫기/다른 페이지로 이동) 전역 도트 숨김
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

            // ✅ 응답 필드명 호환
            const code = it.type ?? it.notificationCode ?? it.code ?? 'NOTI';
            const text =
                it.text ??
                it.message ??
                (code === 'FOLLOW' ? 'Started following you' : code);

            // ✅ 닉네임/아바타
            const actorNick =
                it.actor?.nickname ??
                it.followerNickname ??
                it.actor?.name ??
                it.follower?.nickname ??
                it.following?.follower?.nickname ??
                '';

            const avatarUrl =
                it.actor?.profilePhotoUrl ??
                it.followerProfilePhotoUrl ??
                '/img/profile-default.png';

            const createdAt = it.createdAt ?? it.created_at ?? null;

            // ✅ 읽음 여부(여러 키 호환)
            const read =
                (typeof it.read === 'boolean') ? it.read :
                    (typeof it.checkStatus === 'boolean') ? it.checkStatus :
                        (typeof it.checked === 'boolean') ? it.checked : false;

            // === 카드(전체를 링크로) ===
            const row = document.createElement('a');
            row.className = 'noti-card';
            if (id != null) row.dataset.id = String(id);
            row.href = actorNick ? `/profile/${encodeURIComponent(actorNick)}` : '#';
            row.setAttribute('aria-label', actorNick ? `${actorNick} ${text}` : text);

            // 좌측 빨간 점(미읽음만)
            if (!read) {
                const dot = document.createElement('span');
                dot.className = 'noti-unread-dot';
                row.appendChild(dot);
            }

            // 프로필 이미지
            const img = document.createElement('img');
            img.className = 'noti-avatar';
            img.alt = actorNick ? `${actorNick} profile` : 'profile';
            img.src = avatarUrl || '/img/profile-default.png';
            img.onerror = () => {
                img.src = '/img/profile-default.png';
            };
            row.appendChild(img);

            // 본문 래퍼
            const main = document.createElement('div');
            main.className = 'noti-main';

            // (1줄) 닉네임 + 시간
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

            // (2줄) 메시지
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

            // ▼▼▼ IMPORTANT: 알림 페이지에서는 "떠날 때 도트 제거" 정책을 위해
            // 아래 전역 도트 즉시 갱신 로직을 사용하지 않습니다.
            // if (typeof data?.unreadCount === 'number') {
            //     const dot = document.getElementById('nav-bell-dot');
            //     if (dot) dot.style.display = data.unreadCount > 0 ? 'inline-block' : 'none';
            // } else {
            //     refreshUnreadBadge();
            // }

            // ✅ 진입 후 1회만 전체 읽음 처리(서버 상태만 즉시 갱신)
            if (!readAllOnce) {
                readAllOnce = true;
                try {
                    await fetchJsonFallback('/read-all', {method: 'POST'});
                    // 전역 도트는 "페이지를 떠날 때" 숨김
                    attachLeaveHandlersOnce();
                } catch (e) {
                    console.debug('[notification] read-all failed:', e?.message || e);
                }
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
