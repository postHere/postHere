// /static/js/pages/notification.js
// 역할: 알림 목록 로딩 + (읽음 처리 단일 엔드포인트 연계)
// 템플릿과의 매칭: #list

export function initNotification() {
    // 이 페이지가 아니면 종료
    if (document.body?.id !== 'page-notifications') return;

    // [신규] 웹뷰/브라우저의 자동 스크롤 복원 무력화 (최신 알림을 헤더 바로 밑에 보장)
    try {
        if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
    } catch {
    }

    let readAllOnce = false;

    const $list = document.querySelector('#list');
    const $sentinel = document.getElementById('noti-sentinel'); // [신규] 무한 스크롤 센티넬

    if (!$list) {
        console.warn('[notification] 알림 리스트 요소를 찾지 못했습니다.');
        return;
    }

    function syncLayoutVars() {
        try {
            const navBar = document.querySelector('.main-nav-bar');   // 하단 바 전체
            const hNav = navBar ? Math.ceil(navBar.getBoundingClientRect().height) : 0;
            const navPx = (hNav && Number.isFinite(hNav)) ? `${hNav}px` : null;
            if (navPx) document.documentElement.style.setProperty('--footer-height', navPx);
            // 상단은 헤더 고정이므로 별도 계산 필요 없음(헤더 높이로 CSS가 padding-top 계산)
        } catch { /* ignore */
        }
    }

    syncLayoutVars();
    setTimeout(syncLayoutVars, 300);
    window.addEventListener('resize', syncLayoutVars);
    window.addEventListener('orientationchange', syncLayoutVars);

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

    // === 문구 한글 통일: 타입별 맵핑 ===
    function koreanMessageOf(it) {
        // 가능한 코드 값: FOLLOW, FORUM_COMMENT, COMMENT, etc.
        const code = (it.type ?? it.notificationCode ?? it.code ?? '').toUpperCase();
        if (code === 'FOLLOW') {
            return '님이 회원님을 팔로우하기 시작했습니다';
        }
        if (code === 'FORUM_COMMENT' || code === 'COMMENT') {
            return '님이 회원님의 Forum에 댓글을 남겼습니다';
        }
        // 알 수 없는 타입은 서버 메시지 사용(최후의 보루)
        return (it.text ?? it.message ?? (code || '')) || '';
    }

    // 렌더 한 건
    function renderOne(it) {
        const id = it.id ?? it.notificationId ?? it.notification_pk ?? null;
        const actorNick = it.actor?.nickname ?? it.followerNickname ?? it.actor?.name ?? it.follower?.nickname ?? it.following?.follower?.nickname ?? '';
        const avatarUrl = it.actor?.profilePhotoUrl ?? it.followerProfilePhotoUrl ?? '/images/profile-default.png';
        const createdAt = it.createdAt ?? it.created_at ?? null;
        const read = (typeof it.read === 'boolean') ? it.read
            : (typeof it.checkStatus === 'boolean') ? it.checkStatus
                : (typeof it.checked === 'boolean') ? it.checked : false;

        // 댓글 알림 링크 우선, 없으면 프로필
        const link = it.link || (actorNick ? `/profile/${encodeURIComponent(actorNick)}` : '#');

        const row = document.createElement('a');
        row.className = 'noti-card';
        if (id != null) row.dataset.id = String(id);
        row.href = link;
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
        // @ 제거
        nickEl.textContent = actorNick || '(알 수 없음)';

        const timeEl = document.createElement('span');
        timeEl.className = 'noti-time';
        timeEl.textContent = timeAgo(createdAt);

        head.appendChild(nickEl);
        head.appendChild(timeEl);

        const actionEl = document.createElement('div');
        actionEl.className = 'noti-action';
        actionEl.textContent = koreanMessageOf(it);

        main.appendChild(head);
        main.appendChild(actionEl);

        if (it.commentPreview) {
            const pv = document.createElement('div');
            pv.className = 'noti-preview';
            pv.textContent = String(it.commentPreview);
            main.appendChild(pv);
        }

        row.appendChild(main);
        return row;
    }

    // [신규] 중복 방지용 Set (페이지 경계 중복 안전)
    const seenIds = new Set();

    // 누적 렌더
    function append(items = []) {
        if (!items.length) return;
        const frag = document.createDocumentFragment();
        for (const it of items) {
            const nid = it.id ?? it.notificationId ?? it.notification_pk;
            if (nid != null && seenIds.has(nid)) continue; // [신규] 중복 스킵
            if (nid != null) seenIds.add(nid);
            frag.appendChild(renderOne(it));
        }
        $list.appendChild(frag);
    }

    // === 무한 스크롤 ===
    let page = 0;
    const size = 30;                      // 선로딩 강화
    let loading = false;
    let done = false;

    async function loadNext() {
        if (loading || done) return;
        loading = true;
        try {
            const data = await postJson(`${API_BASE}/list`, {page, size});
            // 서버 DTO: { items: [...], unreadCount: N } (페이지 메타 없음)
            const items = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : [];

            if (page === 0 && !items.length) {
                $list.innerHTML = '';
                const empty = document.createElement('div');
                empty.className = 'empty';
                empty.textContent = '새 알림이 없습니다.';
                $list.appendChild(empty);
                done = true;
                // 첫 진입에 아무것도 없어도 스크롤 0 보정
                requestAnimationFrame(() => requestAnimationFrame(() => window.scrollTo(0, 0)));
                return;
            }

            append(items);
            page += 1;

            // [중요] 초기 진입 직후 헤더 바로 밑에서 시작하도록 2-frame 보정
            if (page === 1) {
                requestAnimationFrame(() => requestAnimationFrame(() => window.scrollTo(0, 0)));
            }

            // hasNext가 응답에 없으므로 휴리스틱:
            //  - hasNext(명시) => 신뢰
            //  - 없으면 items.length > 0인 동안 계속 로드, 빈 배열 받으면 종료
            const respHasNext = (typeof data?.hasNext === 'boolean')
                ? data.hasNext
                : (typeof data?.pageInfo?.hasNext === 'boolean')
                    ? data.pageInfo.hasNext
                    : undefined;

            if (respHasNext === true) {
                // 계속
            } else if (respHasNext === false) {
                done = true;
                if ($sentinel) $sentinel.style.display = 'none';
            } else {
                // 메타가 없으면 '빈 배열 나올 때까지' 계속
                if (items.length === 0) {
                    done = true;
                    if ($sentinel) $sentinel.style.display = 'none';
                }
            }

            if (!readAllOnce) {
                readAllOnce = true;
                try {
                    await postJson('/notification', {}); // 기존 엔드포인트 유지
                    attachLeaveHandlersOnce();
                } catch (e) {
                    console.debug('[notification] read-all failed:', e?.message || e);
                }
            }
        } catch (e) {
            console.error('[notification] load failed:', e?.message || e);
        } finally {
            loading = false;
        }
    }

    if ($sentinel && 'IntersectionObserver' in window) {
        const io = new IntersectionObserver((entries) => {
            for (const ent of entries) {
                if (ent.isIntersecting) loadNext();
            }
        }, {root: null, rootMargin: '1000px 0px', threshold: 0}); // 넉넉한 선로딩
        io.observe($sentinel);
    } else {
        window.addEventListener('scroll', () => {
            const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 800;
            if (nearBottom) loadNext();
        });
    }

    // 최초 로드
    loadNext();
}
