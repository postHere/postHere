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

    async function unreadCountSync() {
        const res = await fetch('/notification/unread-count', {
            method: 'POST', headers: authHeaders({'Accept': 'application/json'})
        });
        if (!res.ok) return;
        const count = await res.json();
        bellDot.style.display = count > 0 ? 'inline-block' : 'none';
    }

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
        markRead(idsToRead);
    }

    // ✅ 행 전체 클릭 → 프로필 이동
    listEl.addEventListener('click', (e) => {
        const row = e.target.closest('.item');
        if (!row) return;
        const actorId = row.getAttribute('data-actor-id');
        if (actorId) location.href = `/profile/${actorId}`;
    });

    async function load() {
        listEl.innerHTML = '<div class="item"><div class="meta"><div class="text">로딩 중...</div></div></div>';
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
    setInterval(unreadCountSync, 15000);
})();
