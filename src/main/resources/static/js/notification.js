/**
 * [알림센터 동작 시퀀스]
 * 1) 페이지 진입 시 /notification/list 호출로 알림 목록 로딩
 * 2) 방금 본 알림 ID들을 /notification/read 로 즉시 읽음 처리 (멱등성 고려)
 *    - 예: URL 쿼리 'focus' 또는 직전 클릭 알림 ID를 우선 포함
 *    - 실패해도 UI는 계속 표시하되, 재시도/토스트 안내 고려
 * 3) 하단 네비 종 아이콘 빨간점은 /notification/unread-count 재호출로 갱신
 *
 * 구현 팁:
 * - 읽음 처리 API는 멱등(idempotent)하도록 서버/클라이언트 설계
 * - 목록 로딩 ↔ 읽음 처리의 레이스컨디션은 후속 unread-count로 최종 동기화
 * - 가시성 변화(visibilitychange) 시 과도한 재호출 방지
 */

/* 역할(정확한 명칭)
 * - 알림 목록 로딩:        POST /notification/list
 * - 선택 알림 읽음 처리:   POST /notification/read (멱등)
 * - 미읽음 카운트 갱신:    POST /notification/unread-count (배지/빨간점)
 * - 알림 행 클릭 시 이동:  GET  /profile/:userId
 */

(function () {
    const listEl = document.getElementById('list');
    const pageEl = document.getElementById('page');
    const prevBtn = document.getElementById('prev');
    const nextBtn = document.getElementById('next');
    const bellDot = document.getElementById('bell-dot');

    let page = 0, size = 20, last = false;

    function authHeaders(base = {}) {
        const h = {...base};
        const token = document.querySelector('meta[name="_csrf"]')?.content || '';
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        if (token) h[headerName] = token;
        return h;
    }

    function dayLabel(iso) {
        const d = new Date(iso);
        const diff = Math.floor((Date.now() - d.getTime()) / (1000 * 60 * 60 * 24));
        return (diff <= 0) ? '0d' : `${diff}d`;
    }

    // 미읽음 카운트 갱신(정확한 명칭: POST /notification/unread-count) → 배지/빨간점 on/off
    async function unreadCountSync() {
        const res = await fetch('/notification/unread-count', {
            method: 'POST', headers: authHeaders({'Accept': 'application/json'})
        });
        if (!res.ok) return;
        const count = await res.json();
        bellDot.style.display = count > 0 ? 'inline-block' : 'none';
    }

    // 선택 알림 읽음 처리(정확한 명칭: POST /notification/read) — 멱등
    async function markRead(ids) {
        if (!ids.length) return;
        const res = await fetch('/notification/read', {
            method: 'POST',
            headers: authHeaders({'Content-Type': 'application/json', 'Accept': 'application/json'}),
            body: JSON.stringify({notificationIds: ids})
        });
        if (res.ok) {
            const remain = await res.json();
            bellDot.style.display = remain > 0 ? 'inline-block' : 'none';
        }
    }

    // 목록 렌더링(+ 방금 본 ID 수집 → markRead 호출)
    function render(items, unreadCount) {
        bellDot.style.display = unreadCount > 0 ? 'inline-block' : 'none';
        if (!items.length) {
            listEl.innerHTML = '<div class="item"><div class="meta"><div class="text">알림이 없습니다.</div></div></div>';
            return;
        }
        const idsToRead = [];
        listEl.innerHTML = items.map(it => `
      <div class="item ${it.read ? 'read' : ''}" data-id="${it.id}" data-actor-id="${it.actor.userId}">
        <div class="dot"></div>
        <img class="avatar" src="${it.actor.profilePhotoUrl || ''}" alt="">
        <div class="meta">
          <div class="row1">
            <span class="nick">${it.actor.nickname}</span>
            <span class="ago">${dayLabel(it.createdAt)}</span>
          </div>
          <div class="text">${it.text}</div>
        </div>
      </div>
    `).join('');

        // 이번 진입에서 처음 확인한 알림 읽음 처리
        items.filter(x => !x.read).forEach(x => idsToRead.push(x.id));
        // render() 내부 : 방금 본 미읽음 읽음 처리
        markRead(idsToRead);
    }

    // 행 전체 클릭 → 프로필 이동(정확한 명칭: GET /profile/:userId)
    listEl.addEventListener('click', (e) => {
        const row = e.target.closest('.item');
        if (!row) return;
        const actorId = row.getAttribute('data-actor-id');
        if (actorId) location.href = `/profile/${actorId}`;
    });

    // 알림 목록 로딩 + 페이지 상태 업데이트(정확한 명칭: POST /notification/list)
    async function load() {
        listEl.innerHTML = '<div class="item"><div class="meta"><div class="text">로딩 중...</div></div></div>';
        // 목록 로딩
        const res = await fetch('/notification/list', {
            method: 'POST',
            headers: authHeaders({'Accept': 'application/json', 'Content-Type': 'application/json'}),
            body: JSON.stringify({page, size})
        });
        if (!res.ok) {
            listEl.innerHTML = `<div class="item"><div class="meta"><div class="text" style="color:#dc2626">HTTP ${res.status}</div></div></div>`;
            return;
        }
        const data = await res.json();
        last = (data.items || []).length < size;
        pageEl.textContent = `페이지 ${page + 1}`;
        prevBtn.disabled = page === 0;
        nextBtn.disabled = last;

        const items = (data.items || []).map(x => ({
            id: x.id, type: x.type, actor: x.actor,
            text: x.text, createdAt: x.createdAt, read: x.read || x.isRead
        }));
        // 렌더 → 내부에서 미읽음 ID 수집
        render(items, data.unreadCount);
    }

    prevBtn.addEventListener('click', () => {
        if (page > 0) {
            page--;
            load();
        }
    });
    nextBtn.addEventListener('click', () => {
        if (!last) {
            page++;
            load();
        }
    });

    load();
    // 별도 주기 싱크
    // 뱃지 갱신 (정확한 명칭: POST /notification/unread-count, 주기적)
    setInterval(unreadCountSync, 15000);
})();
