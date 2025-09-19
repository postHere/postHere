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
    });
    searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        if (isComposing) return;
        state.page.search = 0;
        loadSearch();
    });

    // 페이지네이션
    document.getElementById('prev-followers').addEventListener('click', () => {
        if (state.page.followers > 0) {
            state.page.followers--;
            loadFollowers();
        }
    });
    document.getElementById('next-followers').addEventListener('click', () => {
        state.page.followers++;
        loadFollowers();
    });
    document.getElementById('prev-followings').addEventListener('click', () => {
        if (state.page.followings > 0) {
            state.page.followings--;
            loadFollowings();
        }
    });
    document.getElementById('next-followings').addEventListener('click', () => {
        state.page.followings++;
        loadFollowings();
    });
    document.getElementById('prev-search').addEventListener('click', () => {
        if (state.page.search > 0) {
            state.page.search--;
            loadSearch();
        }
    });
    document.getElementById('next-search').addEventListener('click', () => {
        state.page.search++;
        loadSearch();
    });

    // 상태
    let state = {
        active: 'followers',
        page: {followers: 0, followings: 0, search: 0},
        followingSet: new Set()
    };

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

    function applyPager(el, d, type) {
        el.textContent = `페이지 ${d.number + 1} / ${d.totalPages} (총 ${d.totalElements}명)`;
        document.getElementById(`prev-${type}`).disabled = d.first;
        document.getElementById(`next-${type}`).disabled = d.last;
    }

    async function fetchStatus(ids) {
        if (!ids || !ids.length) return {};
        const res = await fetch('/friend/status', {
            method: 'POST',
            credentials: 'include', // ✅ 세션 쿠키 첨부
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
            credentials: 'include', // ✅ 세션 쿠키 첨부
            headers: authHeaders({'Accept': 'application/json', 'Content-Type': 'application/json'}),
            body: JSON.stringify({userId})
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    async function apiUnfollow(userId) {
        const res = await fetch('/friend/unfollowing', {
            method: 'DELETE',
            credentials: 'include', // ✅ 세션 쿠키 첨부
            headers: authHeaders({'Accept': 'application/json', 'Content-Type': 'application/json'}),
            body: JSON.stringify({userId})
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }

    /**
     * ✅ 유저 행 전체 클릭 → 프로필 이동
     *  - .row에 tabindex="0" + keydown(Enter/Space) 지원
     *  - 팔로우/언팔 버튼 클릭은 event.stopPropagation()으로 전파 차단
     */
    function rowEl(user, mode, statusMap) {
        const row = document.createElement('div');
        row.className = 'row';
        row.setAttribute('tabindex', '0');

        function goProfile() {
            // ✅ 최종 스펙: /profile/{nickname}
            if (user?.nickname) {
                window.location.href = `/profile/${encodeURIComponent(user.nickname)}`;
            } else {
                window.location.href = '/profile'; // 닉네임 없으면 내 프로필로 폴백
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

        // 버튼 maker (전파 차단 포함)
        const makeBtn = (title, svg, onClick) => {
            const b = document.createElement('button');
            b.className = 'icon-btn';
            b.title = title;
            b.innerHTML = svg;
            b.addEventListener('click', (e) => {
                e.stopPropagation();
                onClick();
            });
            b.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') e.stopPropagation();
            });
            return b;
        };

        if (mode === 'followers' || mode === 'search') {
            const already = statusMap ? !!statusMap[user.id] : state.followingSet.has(user.id);
            if (!already) {
                btn = makeBtn('친구 추가', personAddSvg, async () => {
                    try {
                        await apiFollow(user.id);
                        state.followingSet.add(user.id);
                        row.remove();
                    } catch (e) {
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

    function renderList(container, pageData, mode, statusMap) {
        const d = pageData;
        if (!d || !d.content || d.content.length === 0) {
            container.innerHTML = '<div class="empty">결과가 없습니다.</div>';
            return;
        }
        container.innerHTML = '';
        for (const u of d.content) container.appendChild(rowEl(u, mode, statusMap));
    }

    async function loadFollowers() {
        listFollowers.innerHTML = '<div class="empty">로딩 중...</div>';
        const res = await fetch(`/friend/followlist?page=${state.page.followers}&size=20`, {
            credentials: 'include', // ✅ 세션 쿠키 첨부
            headers: authHeaders({Accept: 'application/json'})
        });
        if (!res.ok) return (listFollowers.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res.status}</div>`);
        const d = await res.json();
        const ids = (d.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listFollowers, d, 'followers', status);
        applyPager(pageFollowersEl, d, 'followers');
    }

    async function loadFollowings() {
        listFollowings.innerHTML = '<div class="empty">로딩 중...</div>';
        const res = await fetch(`/friend/followinglist?page=${state.page.followings}&size=20`, {
            credentials: 'include', // ✅ 세션 쿠키 첨부
            headers: authHeaders({Accept: 'application/json'})
        });
        if (!res.ok) return (listFollowings.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res.status}</div>`);
        const d = await res.json();
        if (state.page.followings === 0) state.followingSet = new Set((d.content || []).map(u => u.id));
        renderList(listFollowings, d, 'followings');
        applyPager(pageFollowingsEl, d, 'followings');
    }

    async function loadSearch() {
        const q = (searchInput.value || '').trim();
        const page = state.page.search, size = 20;

        if (!q) {
            listSearch.innerHTML = '<div class="empty">닉네임을 입력해주세요.</div>';
            pageSearchEl.textContent = '';
            document.getElementById('prev-search').disabled = true;
            document.getElementById('next-search').disabled = true;
            return;
        }

        listSearch.innerHTML = '<div class="empty">검색 중...</div>';
        const url = `/friend/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`;
        const res = await fetch(url, {
            credentials: 'include', // ✅ 세션 쿠키 첨부
            headers: authHeaders({Accept: 'application/json'})
        });
        if (!res.ok) {
            listSearch.innerHTML = `<div class="empty" style="color:#dc2626;">HTTP ${res.status}</div>`;
            return;
        }
        const d = await res.json();
        const ids = (d.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listSearch, d, 'search', status);
        applyPager(pageSearchEl, d, 'search');
    }

    // 탭 전환
    tabs.forEach(t =>
        t.addEventListener('click', () => {
            const tab = t.dataset.tab;
            if (state.active === tab) return;
            state.active = tab;
            tabs.forEach(x => x.classList.toggle('active', x.dataset.tab === tab));
            Object.entries(panels).forEach(([k, el]) => el.classList.toggle('active', k === tab));

            if (tab === 'followers') {
                loadFollowers();
            } else if (tab === 'followings') {
                loadFollowings();
            } else {
                // Search 탭 전환 시 포커스
                searchInput.focus();
                // 기존 검색어로 즉시 갱신(선택 사항)
                loadSearch();
            }
        })
    );

    // 초기 로딩
    async function init() {
        listFollowers.innerHTML = listFollowings.innerHTML = '<div class="empty">로딩 중...</div>';
        const [fRes, gRes] = await Promise.all([
            fetch('/friend/followlist?page=0&size=20', {
                credentials: 'include', // ✅ 세션 쿠키 첨부
                headers: authHeaders({Accept: 'application/json'})
            }),
            fetch('/friend/followinglist?page=0&size=20', {
                credentials: 'include', // ✅ 세션 쿠키 첨부
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

        const ids = (followers.content || []).map(u => u.id);
        const status = await fetchStatus(ids);
        renderList(listFollowers, followers, 'followers', status);
        applyPager(pageFollowersEl, followers, 'followers');

        renderList(listFollowings, followings, 'followings');
        applyPager(pageFollowingsEl, followings, 'followings');
    }

    init().catch(error => {
        console.error("초기화 중 오류 발생:", error);
        // 사용자에게 에러 메시지를 보여주는 UI 처리 필요할 수도?
    });
}
