// /static/js/pages/notification.js
// 역할: 알림 목록 로딩 + (읽음 처리 단일 엔드포인트 연계) + 메인 네비 빨간 점(배지) 제어
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
            // [참고] 하단 안전영역(env(safe-area-inset-bottom))은 CSS에서 calc에 포함하여 처리합니다.
        } catch { /* ignore */
        }
    }

    // [추가] 헤더 실제 높이를 측정해 CSS 변수로 반영(얇은 헤더/세로 중앙정렬 유지용)
    function syncHeaderHeightVar() {
        try {
            // .noti-topbar 자체에 safe-area 상단 패딩이 포함되어 있으므로, 헤더의 '보이는 전체 높이'를 사용
            const header = document.querySelector('.noti-topbar');
            const h = header ? Math.ceil(header.getBoundingClientRect().height) : 56;
            const px = (h && Number.isFinite(h)) ? `${h}px` : '56px';
            document.documentElement.style.setProperty('--header-height', px);
        } catch { /* ignore */
        }
    }

    syncFooterHeightVar();
    setTimeout(syncFooterHeightVar, 300);
    window.addEventListener('resize', syncFooterHeightVar);
    window.addEventListener('orientationchange', syncFooterHeightVar);

    // [추가] 헤더 높이도 초기/지연/반응형 시점에 동기화
    syncHeaderHeightVar();
    setTimeout(syncHeaderHeightVar, 300);
    // [추가] 폰트 로딩/레이아웃 확정 이후 한 번 더 실측(타이틀 줄높이 반영)
    window.addEventListener('load', syncHeaderHeightVar);
    window.addEventListener('resize', syncHeaderHeightVar);
    window.addEventListener('orientationchange', syncHeaderHeightVar);

    const API_BASE = '/api/notifications';

    async function postJson(url, bodyObj, signal) {
        const res = await fetch(url, {
            method: 'POST',
            credentials: 'include',
            headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
            body: bodyObj ? JSON.stringify(bodyObj) : '{}',
            signal
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

    // ===== 빨간 점(배지) 관련 메서드 =====
    function setNavDotVisible(visible) {
        const dot = document.getElementById('nav-bell-dot');
        if (dot) dot.style.display = visible ? 'inline-block' : 'none';
    }

    async function refreshNavDotFromServer() {
        try {
            const count = await postJson(`${API_BASE}/unread-count`, {});
            setNavDotVisible(Number(count) > 0);
        } catch (e) {
            // 서버 오류 시 배지 상태는 유지
            console.debug('[notification] unread-count failed:', e?.message || e);
        }
    }

    function attachLeaveHandlersOnce() {
        const hide = () => setNavDotVisible(false);
        window.addEventListener('pagehide', hide, {once: true});
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) hide();
        }, {once: true});
        window.addEventListener('beforeunload', hide, {once: true});
    }

    // [추가] 코드→한국어 문구 매핑(요청된 고정 문구 적용)
    function mapText(code) {
        switch (code) {
            case 'FOLLOW':
                return '회원님을 팔로우하기 시작했습니다';
            case 'COMMENT':
            case 'FORUM_COMMENT':
                return '회원님의 Forum에 댓글을 남겼습니다';
            default:
                return '알림';
        }
    }

    function render(items = [], {append = false} = {}) {
        if (!append) $list.innerHTML = '';
        if (!items.length && !append) {
            const empty = document.createElement('div');
            empty.className = 'empty';
            empty.textContent = '새 알림이 없습니다.';
            $list.appendChild(empty);
            // 목록이 비었을 때도 서버 카운트와 동기화
            refreshNavDotFromServer();
            return;
        }

        const frag = document.createDocumentFragment();
        let hasUnreadInBatch = false;

        items.forEach((it) => {
            const id = it.id ?? it.notificationId ?? it.notification_pk ?? null;
            const code = it.type ?? it.notificationCode ?? it.code ?? null;
            const createdAt = it.createdAt ?? it.created_at ?? null;
            const read =
                (typeof it.isRead === 'boolean') ? it.isRead :
                    (typeof it.read === 'boolean') ? it.read :
                        (typeof it.checkStatus === 'boolean') ? it.checkStatus :
                            (typeof it.checked === 'boolean') ? it.checked : false;

            if (!read) hasUnreadInBatch = true;

            // 공통 변수
            let fixedText = '';
            let actorNick = '';
            let avatarUrl = '';
            let link = it.link || '#';

            if (code === 'FIND_FOUND') {
                // FIND_FOUND 전용: 텍스트만(아바타 없음)
                fixedText = it.text || '습득물이 발견되었습니다.';
                const row = document.createElement('a');
                row.className = 'noti-card';
                if (id != null) row.dataset.id = String(id);
                row.href = link;
                row.setAttribute('aria-label', fixedText);
                row.dataset.unread = String(!read);

                const main = document.createElement('div');
                main.className = 'noti-main';

                const head = document.createElement('div');
                head.className = 'noti-head';

                const timeEl = document.createElement('span');
                timeEl.className = 'noti-time';
                timeEl.textContent = timeAgo(createdAt);

                head.appendChild(timeEl);

                const textEl = document.createElement('div');
                textEl.className = 'noti-text';
                textEl.textContent = fixedText;

                main.appendChild(head);
                main.appendChild(textEl);
                row.appendChild(main);
                frag.appendChild(row);
                return; // 다음 아이템
            }

            // 일반 알림
            fixedText = mapText(code);
            actorNick =
                it.actor?.nickname ??
                it.followerNickname ??
                it.actor?.name ??
                it.follower?.nickname ??
                it.following?.follower?.nickname ??
                '';
            avatarUrl = it.actor?.profilePhotoUrl ?? it.followerProfilePhotoUrl ?? '/images/profile-default.png';

            // FOLLOW 등 link가 없으면 프로필로
            if (!it.link && actorNick) {
                link = `/profile/${encodeURIComponent(actorNick)}`;
            }

            const row = document.createElement('a');
            row.className = 'noti-card';
            if (id != null) row.dataset.id = String(id);
            row.href = link;
            row.setAttribute('aria-label', actorNick ? `${actorNick} ${fixedText}` : fixedText);
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
            nickEl.textContent = actorNick || '(알 수 없음)';

            const timeEl = document.createElement('span');
            timeEl.className = 'noti-time';
            timeEl.textContent = timeAgo(createdAt);

            head.appendChild(nickEl);
            head.appendChild(timeEl);

            const textEl = document.createElement('div');
            textEl.className = 'noti-text';
            textEl.textContent = String(fixedText);

            main.appendChild(head);
            main.appendChild(textEl);

            if (it.commentPreview) {
                const pv = document.createElement('div');
                pv.className = 'noti-preview';
                pv.textContent = String(it.commentPreview);
                main.appendChild(pv);
            }

            row.appendChild(main);
            frag.appendChild(row);
        });

        $list.appendChild(frag);

        // 현재 배치에 미읽음이 있으면 종 아이콘 배지도 켬 (서버 카운트 동기화 전 즉시 반영)
        if (hasUnreadInBatch) {
            setNavDotVisible(true);
        } else {
            // 배치가 모두 읽음 표시라면 서버 카운트와 동기화
            refreshNavDotFromServer();
        }
    }

    // ====== 무한 스크롤 로딩 ======
    const PAGE_SIZE = 30;
    let page = 0;
    let isLoading = false;
    let isLast = false;
    let aborter = null;

    async function load({append = false} = {}) {
        if (isLoading || isLast) return;
        isLoading = true;

        try {
            aborter = new AbortController();
            const data = await postJson(`${API_BASE}/list`, {page, size: PAGE_SIZE}, aborter.signal);
            const items = Array.isArray(data?.items) ? data.items : Array.isArray(data) ? data : [];

            render(items, {append});

            // [수정] 페이지네이션 메타가 없을 수 있어 size로 종료 판단
            if (items.length < PAGE_SIZE) {
                isLast = true;
            } else {
                page += 1;
            }

            // 최초 진입 시 한 번만 전체 읽음 처리 트리거(팀의 기존 엔드포인트 유지)
            if (!readAllOnce) {
                readAllOnce = true;
                try {
                    // 기존 호환 엔드포인트: '/notification' (서버에서 read-all 처리하도록 구성되어 있음)
                    await postJson('/notification', {});
                    // 읽음 처리 직후 종 아이콘 배지 OFF + leave handler 등록
                    setNavDotVisible(false);
                    attachLeaveHandlersOnce();
                } catch (e) {
                    console.debug('[notification] read-all failed:', e?.message || e);
                }
            }
        } catch (e) {
            console.error('[notification] load failed:', e?.message || e);
            if (!append) render([]);
        } finally {
            isLoading = false;
            aborter = null;
        }
    }

    // [추가] overscroll bounce(부드럽게) - 리스트만 적용, nav는 고정
    function attachBounce(scrollEl) {
        if (!scrollEl) return;
        let startY = 0;
        let pulling = 0;
        let isTouching = false;
        const maxPull = 80;
        const damp = 0.25;

        const onTouchStart = (e) => {
            isTouching = true;
            startY = (e.touches ? e.touches[0].clientY : e.clientY);
            pulling = 0;
            scrollEl.style.transition = 'transform 0s';
        };
        const onTouchMove = (e) => {
            if (!isTouching) return;
            const y = (e.touches ? e.touches[0].clientY : e.clientY);
            const dy = y - startY;

            const atTop = scrollEl.scrollTop <= 0;
            const atBottom = scrollEl.scrollTop + scrollEl.clientHeight >= scrollEl.scrollHeight - 1;

            let offset = 0;
            if (dy > 0 && atTop) {
                offset = Math.min(maxPull, dy * damp);
            } else if (dy < 0 && atBottom) {
                offset = Math.max(-maxPull, dy * damp);
            }
            pulling = offset;
            if (offset !== 0) {
                e.preventDefault();
                scrollEl.style.transform = `translateY(${offset}px)`;
            }
        };
        const onTouchEnd = () => {
            isTouching = false;
            if (pulling !== 0) {
                scrollEl.style.transition = 'transform 200ms ease-out';
                scrollEl.style.transform = 'translateY(0)';
            }
            pulling = 0;
        };

        scrollEl.addEventListener('touchstart', onTouchStart, {passive: false});
        scrollEl.addEventListener('touchmove', onTouchMove, {passive: false});
        scrollEl.addEventListener('touchend', onTouchEnd, {passive: true});
    }

    // [추가] 무한 스크롤 트리거(리스트 스크롤)
    function attachInfiniteScroll(scrollEl) {
        const onScroll = () => {
            if (isLoading || isLast) return;
            const threshold = 300;
            const nearBottom = scrollEl.scrollTop + scrollEl.clientHeight >= scrollEl.scrollHeight - threshold;
            if (nearBottom) {
                load({append: true});
            }
        };
        scrollEl.addEventListener('scroll', onScroll, {passive: true});
        scrollEl.__infHandler = onScroll;
    }

    function detachInfiniteScroll(scrollEl) {
        const h = scrollEl?.__infHandler;
        if (!h) return;
        scrollEl.removeEventListener('scroll', h);
        delete scrollEl.__infHandler;
    }

    // ===== 초기 실행 =====
    // 1) 메인 네비 배지 상태를 서버 카운트로 동기화
    refreshNavDotFromServer();

    // 2) 리스트 스크롤/바운스/로드
    attachBounce($list);
    attachInfiniteScroll($list);
    load({append: false});

    // [안정성] 페이지 떠날 때 로딩 취소
    window.addEventListener('pagehide', () => {
        try {
            aborter?.abort();
        } catch {
        }
        detachInfiniteScroll($list);
    }, {once: true});
}
