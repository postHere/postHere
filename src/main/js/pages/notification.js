// /static/js/pages/notification.js
// 역할: 알림 목록 로딩 + 페이징 + (읽음 처리 단일 엔드포인트 연계)
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

    // ===== 뒤로가기 버튼 처리 (좌상단 아이콘) =====
    const $back = document.getElementById('noti-back');
    if ($back) {
        $back.addEventListener('click', (e) => {
            e.preventDefault();
            // 같은 오리진 & 히스토리가 있으면 back, 아니면 /forumMain 폴백
            if (document.referrer && document.referrer.startsWith(location.origin) && history.length > 1) {
                history.back();
            } else {
                location.assign('/forumMain');
            }
        });
    }

    // ===== ▼ (있다면) 네비 높이를 CSS 변수(--footer-height)에 반영 =====
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
    // ===== ▲ 추가 끝 =====

    // 백엔드 엔드포인트
    const API_BASE = '/api/notifications'; // 목록/카운트 전용
    // 읽음 처리(전체/선택)는 POST /notification 단일 엔드포인트 사용

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
                '/images/profile-default.png'; // ✅ 기본 경로 확정

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

            // ✅ 빨간 점: DOM 생성 대신 data-unread 속성 사용(정렬 고정)
            row.dataset.unread = String(!read);

            // 프로필 이미지
            const img = document.createElement('img');
            img.className = 'noti-avatar';
            img.alt = actorNick ? `${actorNick} profile` : 'profile';
            img.src = avatarUrl || '/images/profile-default.png';
            img.onerror = () => {
                img.src = '/images/profile-default.png';
            };
            row.appendChild(img);

            // 본문 래퍼
            const main = document.createElement('div');
            main.className = 'noti-main';

            // (1줄) 닉네임 + 시간 (같은 줄)
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

        // ❌ (변경) 여기서는 도트를 숨기지 않는다.
        // setNavDotVisible(false);
    }

    async function load(p = 0) {
        try {
            const data = await postJson(`${API_BASE}/list`, {page: p, size});

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

            // ✅ 진입 후 1회만 전체 읽음 처리(서버 상태만 즉시 갱신)
            if (!readAllOnce) {
                readAllOnce = true;
                try {
                    await postJson('/notification', {}); // 단일 엔드포인트 (suffix 금지)

                    // ❌ (변경) 입장만으로는 종 도트를 숨기지 않는다.
                    // setNavDotVisible(false);

                    // 전역 도트는 "페이지를 떠날 때"만 숨김 보장
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
        // ✅ 내부 페이지 이동 시에만 종 도트를 숨김
        setNavDotVisible(false);
        if (page > 0) load(page - 1);
    });
    $btnNext.addEventListener('click', () => {
        setNavDotVisible(false);
        if (!last) load(page + 1);
    });

    // 최초 로드
    load(0);
}
