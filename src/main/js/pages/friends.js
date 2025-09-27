/**
 * friends.js
 * - 팔로워/팔로잉/검색 탭
 * - 팔로우/언팔로우 트리거
 * - 프로필 이동
 */
export function initFriends() {

    const tabs = document.querySelectorAll('.tab');
    const panels = {
        followers: document.getElementById('panel-followers'),
        followings: document.getElementById('panel-followings'),
        search: document.getElementById('panel-search')
    };
    const listFollowers = document.getElementById('list-followers');
    const listFollowings = document.getElementById('list-followings');
    const listSearch = document.getElementById('list-search');
    const pageFollowersEl = document.getElementById('page-followers');
    const pageFollowingsEl = document.getElementById('page-followings');
    const pageSearchEl = document.getElementById('page-search');

    // Enter 검색(Form submit) + IME 안전
    const searchForm = document.getElementById('searchForm');
    const searchInput = document.getElementById('searchInput');
    let isComposing = false;
    searchInput.addEventListener('compositionstart', () => {
        isComposing = true;
    });
    searchInput.addEventListener('compositionend', () => {
        isComposing = false;
        // [수정] 조합 종료 시점에도 즉시 검색 재시도
        state.page.search = 0;
        state.last.search = false;
        cancelInFlight('search');
        loadSearch({replace: true});
    });
    searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        // [수정] IME 여부와 무관하게 엔터는 항상 검색
        state.page.search = 0;
        state.last.search = false; // ★ 이전 빈쿼리로 true였던 상태 해제
        cancelInFlight('search');
        loadSearch({replace: true});
    });

    // [수정] 입력할 때마다 서버 검색(IME 중에도 수행) + 디바운스 120ms + 종료 플래그 리셋
    let searchDebounceTimer = null;
    const triggerLiveSearch = () => {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            state.page.search = 0;
            state.last.search = false; // ★ 입력 시작과 동시에 재탐색 가능
            cancelInFlight('search');
            loadSearch({replace: true}); // forum-area-search처럼 즉시 갱신
        }, 120);
    };
    searchInput.addEventListener('input', triggerLiveSearch);
    // [수정] 조합 중 글자 변할 때도 반영
    searchInput.addEventListener('compositionupdate', triggerLiveSearch);

    // 페이지네이션
    document.getElementById('prev-followers').addEventListener('click', () => {
        if (state.page.followers > 0) {
            state.page.followers--;
            cancelInFlight('followers');
            loadFollowers({replace: true});
        }
    });
    document.getElementById('next-followers').addEventListener('click', () => {
        state.page.followers++;
        cancelInFlight('followers');
        loadFollowers({append: true});
    });
    document.getElementById('prev-followings').addEventListener('click', () => {
        if (state.page.followings > 0) {
            state.page.followings--;
            cancelInFlight('followings');
            loadFollowings({replace: true});
        }
    });
    document.getElementById('next-followings').addEventListener('click', () => {
        state.page.followings++;
        cancelInFlight('followings');
        loadFollowings({append: true});
    });
    document.getElementById('prev-search').addEventListener('click', () => {
        if (state.page.search > 0) {
            state.page.search--;
            cancelInFlight('search');
            loadSearch({replace: true});
        }
    });
    document.getElementById('next-search').addEventListener('click', () => {
        state.page.search++;
        cancelInFlight('search');
        loadSearch({append: true});
    });

    // 상태
    let state = {
        active: 'followers',
        page: {followers: 0, followings: 0, search: 0},
        followingSet: new Set(),
        last: {followers: false, followings: false, search: false},
        isLoading: {followers: false, followings: false, search: false},
        dirty: {followers: false, followings: false, search: false}
    };

    // 탭별 진행 중 요청 취소용 AbortController
    const aborters = {
        followers: null,
        followings: null,
        search: null
    };

    function cancelInFlight(kind) {
        try {
            const a = aborters[kind];
            if (a) a.abort();
        } catch {
        }
        aborters[kind] = null;
        state.isLoading[kind] = false;
    }

    // 아이콘
    const personAddSvg = `
<svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M15 8a4 4 0 1 0-8 0 4 4 0 0 0 8 0Z" stroke="currentColor" stroke-width="2"/>
    <path d="M3.5 20a7 7 0 0 1 14 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
    <path d="M19 8v6m-3-3h6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>`;

    const personRemoveSvg = `
<svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M15 8a4 4 0 1 0-8 0 4 4 0 0 0 8 0Z" stroke="currentColor" stroke-width="2"/>
    <path d="M3.5 20a7 7 0 0 1 14 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
    <path d="m17 10 4 4m0-4-4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>`;

    // 헬퍼
    function authHeaders(base = {}) {
        const headers = {...base};
        const token = document.querySelector('meta[name="_csrf"]')?.content || '';
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        if (token) headers[headerName] = token;
        return headers;
    }

    function applyPager(el, d, type, expectedSize = 20) {
        el.textContent = `페이지 ${d.number + 1} / ${d.totalPages} (총 ${d.totalElements}명)`;
        document.getElementById(`prev-${type}`).disabled = d.first;
        document.getElementById(`next-${type}`).disabled = d.last;

        // [수정] 서버가 last를 안주면 길이 기반으로 보정
        let lastFlag = !!d.last;
        if (!('last' in d)) {
            const arr = Array.isArray(d.content) ? d.content : (Array.isArray(d.items) ? d.items : []);
            lastFlag = arr.length < expectedSize;
        }
        state.last[type] = lastFlag;
    }

    async function fetchStatus(ids) {
        if (!ids || !ids.length) return {};
        const res = await fetch('/friend/status', {
            method: 'POST',
            credentials: 'include',
            headers: authHeaders({'Content-Type': 'application/json', 'Accept': 'application/json'}),
            body: JSON.stringify({ids})
        });
        if (!res.ok) return {};
        const data = await res.json();
        return data?.status || {};
    }

    async function apiFollow(userId) {
        const res = await fetch('/friend/addfollowing', {
            method: 'POST',
            credentials: 'include',
            headers: authHeaders({'Accept': 'application/json', 'Content-Type': 'application/json'}),
            body: JSON.stringify({userId})
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    async function apiUnfollow(userId) {
        const res = await fetch('/friend/unfollowing', {
            method: 'DELETE',
            credentials: 'include',
            headers: authHeaders({'Accept': 'application/json', 'Content-Type': 'application/json'}),
            body: JSON.stringify({userId})
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    /**
     * ✅ 유저 행 전체 클릭 → 프로필 이동
     * - 팔로우/언팔 버튼 클릭은 stopPropagation()으로 차단
     */
    function rowEl(user, mode, statusMap) {
        const row = document.createElement('div');
        row.className = 'row';
        row.setAttribute('tabindex', '0');

        function goProfile() {
            if (user?.nickname) {
                window.location.href = `/profile/${encodeURIComponent(user.nickname)}`;
            } else {
                window.location.href = '/profile';
            }
        }

        row.addEventListener('click', goProfile);
        row.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                goProfile();
            }
        });

        const img = document.createElement('img');
        img.className = 'avatar';
        img.src = user.profilePhotoUrl || `https://picsum.photos/seed/${user.id}/100/100`;
        img.alt = '';
        img.onerror = () => (img.style.visibility = 'hidden');

        const meta = document.createElement('div');
        meta.className = 'meta';
        const name = document.createElement('div');
        name.className = 'name';
        name.textContent = user.nickname || '(이름없음)';
        meta.appendChild(name);

        const spacer = document.createElement('div');
        spacer.className = 'spacer';
        let btn = null;

        // 버튼 maker
        const makeBtn = (title, svg, onClick) => {
            const b = document.createElement('button');
            b.className = 'icon-btn';
            b.title = title;
            b.innerHTML = svg;
            b.addEventListener('click', (e) => {
                e.stopPropagation();
                onClick(b);
            });
            return b;
        };

        if (mode === 'followers' || mode === 'search') {
            const already = statusMap ? !!statusMap[user.id] : state.followingSet.has(user.id);
            if (!already) {
                btn = makeBtn('친구 추가', personAddSvg, async (buttonEl) => {
                    try {
                        buttonEl.disabled = true;
                        await apiFollow(user.id);
                        state.followingSet.add(user.id);

                        // ✅ 버튼만 숨김 처리 (내부 비우지 않음)
                        buttonEl.classList.add('vanish');

                        // [수정] Followings 탭 즉시 새로고침 유도
                        state.dirty.followings = true;
                    } catch (e) {
                        buttonEl.disabled = false;
                        alert('팔로우 실패: ' + (e.message || ''));
                    }
                });
            }
        } else if (mode === 'followings') {
            btn = makeBtn('친구 삭제', personRemoveSvg, async () => {
                try {
                    await apiUnfollow(user.id);
                    state.followingSet.delete(user.id);
                    row.remove();
                    state.dirty.followers = true;
                    state.dirty.search = true; // [유지] 검색 탭도 더러움 표시
                } catch (e) {
                    alert('언팔로우 실패: ' + (e.message || ''));
                }
            });
        }

        row.appendChild(img);
        row.appendChild(meta);
        row.appendChild(spacer);
        if (btn) row.appendChild(btn);
        return row;
    }

    function renderList(container, pageData, mode, statusMap, {append = false} = {}) {
        const d = pageData;
        const content = d?.content || [];
        if (!append) container.innerHTML = '';
        if (!content.length && !append) {
            container.innerHTML = '<div class="empty">결과가 없습니다.</div>';
            return;
        }
        for (const u of content) container.appendChild(rowEl(u, mode, statusMap));
    }

    async function loadFollowers({replace = false, append = false} = {}) {
        if (state.isLoading.followers || state.last.followers) return;
        state.isLoading.followers = true;

        const page = state.page.followers;
        const size = 20;

        const ac = new AbortController();
        aborters.followers = ac;

        if (replace) listFollowers.innerHTML = '<div class="empty">로딩 중...</div>';

        const res = await fetch(`/friend/followlist?page=${page}&size=${size}`, {
            credentials: 'include',
            headers: authHeaders({Accept: 'application/json'}),
            signal: ac.signal
        }).catch(() => null);

        if (!res || !res.ok) {
            if (replace) listFollowers.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res ? res.status : 'ERR'}</div>`;
            state.isLoading.followers = false;
            return;
        }
        const d = await res.json();

        // [수정] 서버가 last 미제공 시 길이로 보정
        if (!('last' in d)) d.last = Array.isArray(d.content) ? (d.content.length < size) : true;

        const ids = (d.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listFollowers, d, 'followers', status, {append});

        applyPager(pageFollowersEl, d, 'followers', size);

        state.isLoading.followers = false;
        aborters.followers = null;
    }

    async function loadFollowings({replace = false, append = false} = {}) {
        if (state.isLoading.followings || state.last.followings) return;
        state.isLoading.followings = true;

        const page = state.page.followings;
        const size = 20;

        const ac = new AbortController();
        aborters.followings = ac;

        if (replace) listFollowings.innerHTML = '<div class="empty">로딩 중...</div>';

        const res = await fetch(`/friend/followinglist?page=${page}&size=${size}`, {
            credentials: 'include',
            headers: authHeaders({Accept: 'application/json'}),
            signal: ac.signal
        }).catch(() => null);

        if (!res || !res.ok) {
            if (replace) listFollowings.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res ? res.status : 'ERR'}</div>`;
            state.isLoading.followings = false;
            return;
        }
        const d = await res.json();
        if (state.page.followings === 0) state.followingSet = new Set((d.content || []).map(u => u.id));

        // [수정] 서버가 last 미제공 시 길이로 보정
        if (!('last' in d)) d.last = Array.isArray(d.content) ? (d.content.length < size) : true;

        renderList(listFollowings, d, 'followings', null, {append});
        applyPager(pageFollowingsEl, d, 'followings', size);

        state.isLoading.followings = false;
        aborters.followings = null;
    }

    async function loadSearch({replace = false, append = false} = {}) {
        const q = (searchInput.value || '').trim();
        const page = state.page.search, size = 20;

        // [수정] 빈 쿼리면 "안내 문구"도 없이 완전히 비움 + 무한스크롤 해제
        if (!q) {
            listSearch.innerHTML = '';
            pageSearchEl.textContent = '';
            document.getElementById('prev-search').disabled = true;
            document.getElementById('next-search').disabled = true;
            state.last.search = true; // 빈 쿼리면 더 불러오지 않음
            detachInfiniteScroll(listSearch); // ★ 스크롤 리스너 제거
            return;
        }

        if (state.isLoading.search || (state.last.search && append)) return;
        state.isLoading.search = true;

        const ac = new AbortController();
        aborters.search = ac;

        if (replace) listSearch.innerHTML = '<div class="empty">검색 중...</div>';

        const url = `/friend/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`;
        const res = await fetch(url, {
            credentials: 'include',
            headers: authHeaders({Accept: 'application/json'}),
            signal: ac.signal
        }).catch(() => null);

        if (!res || !res.ok) {
            if (replace) listSearch.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res ? res.status : 'ERR'}</div>`;
            state.isLoading.search = false;
            return;
        }
        const d = await res.json();

        // [수정] 서버가 last 미제공 시 길이로 보정
        if (!('last' in d)) d.last = Array.isArray(d.content) ? (d.content.length < size) : true;

        const ids = (d.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listSearch, d, 'search', status, {append});
        applyPager(pageSearchEl, d, 'search', size);

        state.isLoading.search = false;
        aborters.search = null;
    }

    // overscroll bounce(부드럽게) - 각 .list 전용 (nav는 고정, 흔들리지 않음)
    function attachBounce(scrollEl) {
        if (!scrollEl) return;
        let startY = 0;
        let pulling = 0;
        let isTouching = false;
        const maxPull = 80; // 최대 당김
        const damp = 0.25;  // 감쇠(부드럽게)

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

        scrollEl.__bounceHandlers = {onTouchStart, onTouchMove, onTouchEnd};
        scrollEl.addEventListener('touchstart', onTouchStart, {passive: false});
        scrollEl.addEventListener('touchmove', onTouchMove, {passive: false});
        scrollEl.addEventListener('touchend', onTouchEnd, {passive: true});
    }

    function detachBounce(scrollEl) {
        const h = scrollEl?.__bounceHandlers;
        if (!h) return;
        scrollEl.removeEventListener('touchstart', h.onTouchStart);
        scrollEl.removeEventListener('touchmove', h.onTouchMove);
        scrollEl.removeEventListener('touchend', h.onTouchEnd);
        delete scrollEl.__bounceHandlers;
        scrollEl.style.transform = 'translateY(0)';
        scrollEl.style.transition = '';
    }

    // 무한스크롤: 각 .list 끝 300px 근처에서 다음 페이지 자동 로드
    function attachInfiniteScroll(scrollEl, kind, loader) {
        const onScroll = () => {
            if (state.isLoading[kind] || state.last[kind]) return;
            const threshold = 300;
            const nearBottom = scrollEl.scrollTop + scrollEl.clientHeight >= scrollEl.scrollHeight - threshold;
            if (nearBottom) {
                state.page[kind] += 1;
                loader({append: true});
            }
        };
        scrollEl.__infHandler = onScroll;
        scrollEl.addEventListener('scroll', onScroll, {passive: true});
    }

    function detachInfiniteScroll(scrollEl) {
        const h = scrollEl?.__infHandler;
        if (!h) return;
        scrollEl.removeEventListener('scroll', h);
        delete scrollEl.__infHandler;
    }

    // 하단 고정 네비 높이를 CSS 변수로 주입(가려짐 방지)
    function syncFooterHeightVar() {
        try {
            const nav = document.querySelector('.main-nav-bar');
            const h = nav ? Math.ceil(nav.getBoundingClientRect().height) : 0;
            const px = (h && Number.isFinite(h)) ? `${h}px` : null;
            if (px) document.documentElement.style.setProperty('--footer-height', px);
        } catch { /* ignore */
        }
    }

    // 탭 전환
    tabs.forEach(t =>
        t.addEventListener('click', () => {
            const tab = t.dataset.tab;
            if (state.active === tab) return;

            // 현재 활성 리스트에서 바운스/무한스크롤 해제
            const prevList = ({
                followers: listFollowers,
                followings: listFollowings,
                search: listSearch
            })[state.active];
            detachBounce(prevList);
            detachInfiniteScroll(prevList);

            state.active = tab;
            tabs.forEach(x => x.classList.toggle('active', x.dataset.tab === tab));
            Object.entries(panels).forEach(([k, el]) => el.classList.toggle('active', k === tab));

            // 새 활성 리스트에 바운스/무한스크롤 부착
            const curList = ({
                followers: listFollowers,
                followings: listFollowings,
                search: listSearch
            })[tab];

            attachBounce(curList);

            if (tab === 'followers') {
                if (state.dirty.followers) {
                    state.page.followers = 0;
                    state.last.followers = false;
                    listFollowers.innerHTML = '';
                    cancelInFlight('followers');
                    loadFollowers({replace: true});
                    state.dirty.followers = false;
                } else if (!listFollowers.children.length) {
                    loadFollowers({replace: true});
                }
                attachInfiniteScroll(curList, 'followers', loadFollowers);
            } else if (tab === 'followings') {
                if (state.dirty.followings) {
                    state.page.followings = 0;
                    state.last.followings = false;
                    listFollowings.innerHTML = '';
                    cancelInFlight('followings');
                    loadFollowings({replace: true});
                    state.dirty.followings = false;
                } else if (!listFollowings.children.length) {
                    loadFollowings({replace: true});
                }
                attachInfiniteScroll(curList, 'followings', loadFollowings);
            } else {
                searchInput.focus();

                // [수정] 탭 진입 시 무조건 "현재 입력값"으로 재검색하여 최신 상태 보장
                cancelInFlight('search');
                state.page.search = 0;
                state.last.search = false;
                const qNow = (searchInput.value || '').trim();
                if (!qNow) {
                    listSearch.innerHTML = '';         // 빈 쿼리면 아무것도 표시하지 않음
                    pageSearchEl.textContent = '';
                    detachInfiniteScroll(curList);     // 무한스크롤 제거
                } else {
                    loadSearch({replace: true});       // 즉시 재검색
                    attachInfiniteScroll(curList, 'search', loadSearch);
                }
                state.dirty.search = false; // 플래그 소거
            }
        })
    );

    // 초기 로딩
    async function init() {
        syncFooterHeightVar();
        setTimeout(syncFooterHeightVar, 300);
        window.addEventListener('resize', syncFooterHeightVar);
        window.addEventListener('orientationchange', syncFooterHeightVar);

        listFollowers.innerHTML = listFollowings.innerHTML = '<div class="empty">로딩 중...</div>';

        const [fRes, gRes] = await Promise.all([
            fetch('/friend/followlist?page=0&size=20', {
                credentials: 'include',
                headers: authHeaders({Accept: 'application/json'})
            }),
            fetch('/friend/followinglist?page=0&size=20', {
                credentials: 'include',
                headers: authHeaders({Accept: 'application/json'})
            })
        ]);
        if (!fRes.ok || !gRes.ok) {
            const msg = `초기 로딩 실패: followers=${fRes.status}, followings=${gRes.status}`;
            listFollowers.innerHTML = listFollowings.innerHTML = `<div class="empty" style="color:#dc2626;">${msg}</div>`;
            return;
        }
        const [followers, followings] = await Promise.all([fRes.json(), gRes.json()]);
        state.followingSet = new Set((followings.content || []).map(u => u.id));

        // [수정] 서버가 last 미제공 시 길이로 보정
        if (!('last' in followers)) followers.last = Array.isArray(followers.content) ? (followers.content.length < 20) : true;
        if (!('last' in followings)) followings.last = Array.isArray(followings.content) ? (followings.content.length < 20) : true;

        const ids = (followers.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listFollowers, followers, 'followers', status, {append: false});
        applyPager(pageFollowersEl, followers, 'followers', 20);

        renderList(listFollowings, followings, 'followings', null, {append: false});
        applyPager(pageFollowingsEl, followings, 'followings', 20);

        // 최초 활성 탭 리스트에 바운스/무한스크롤 부착
        attachBounce(listFollowers);
        attachInfiniteScroll(listFollowers, 'followers', loadFollowers);
    }

    init().catch(error => {
        console.error("초기화 중 오류 발생:", error);
    });
}
