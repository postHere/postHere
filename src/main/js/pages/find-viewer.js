// find-viewer.js (교체본) — 서버가 렌더해 둔 DOM을 읽어 스택 뷰로 변환
export function initFindViewer() {
    console.log('[find-viewer] initFindViewer start');
    // 1) DOM에서 데이터 수집
    const sc = document.getElementById('swipe-container');
    if (!sc) return;

    const startIndex = Number(sc.getAttribute('data-start-index') || 0);

    // 렌더된 카드들을 읽어서 items 배열 생성
    const serverCards = Array.from(sc.querySelectorAll('.card'));
    console.log('[find-viewer] serverCards:', serverCards.length);  //카드 개수 확인
    const items = serverCards.map((el) => {
        const id = Number(el.getAttribute('data-find-id'));
        const imgEl = el.querySelector('.image-container img');
        const profileImg = el.querySelector('.writer-info img')?.getAttribute('src') || '';
        const writer = el.querySelector('.nickname')?.textContent?.trim() || '';
        const location = el.querySelector('.footer span')?.textContent?.trim() || '';
        const timeBlock = el.querySelector('.footer div');
        const createdAt = timeBlock ? (timeBlock.childNodes[0]?.textContent?.trim() || '') : '';
        const remaining = timeBlock ? (timeBlock.childNodes[2]?.textContent?.trim() || '') : '';
        const canDelete = !!el.querySelector('.delete-btn');

        return {
            id,
            imageUrl: imgEl ? imgEl.getAttribute('src') : '',
            writerName: writer,
            writerPhoto: profileImg,
            locationName: location,
            createdAt,
            remainingTime: remaining,
            isAuthor: canDelete
        };
    });

    // 2) 시작 인덱스에 맞춰 카드 순서 재정렬
    if (items.length > 0 && startIndex > 0 && startIndex < items.length) {
        const head = items.splice(0, startIndex);
        items.push(...head);
    }

    // 3) 스택 컨테이너 준비 (#findStack가 없으면 생성)
    let stackEl = document.getElementById('findStack');
    if (!stackEl) {
        stackEl = document.createElement('div');
        stackEl.id = 'findStack';
        stackEl.className = 'stack';
        // 기존 swipe-container(DOM 템플릿)는 더 이상 필요 없으니 비우고 stack만 추가
        const wrapper = sc.parentElement; // .swipe-wrapper
        sc.innerHTML = '';
        wrapper.appendChild(stackEl);
    }

    // XSS 방지를 위한 HTML 이스케이프 유틸리티
    const esc = (s) => String(s ?? '').replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[m]));

    /**
     * 카드 1개의 HTML 템플릿을 생성합니다.
     * @param {object} item - 카드 데이터
     * @param {number} pos - 스택에서의 위치 (0: 맨 위, 1: 중간, 2: 맨 아래)
     * @returns {string} - 생성된 HTML 문자열
     */
    function cardTpl(item, pos = 0) {
        const showBadge = item.remainingTime && !/만료/.test(item.remainingTime);
        return `
      <article class="card" data-id="${esc(item.id)}" data-pos="${pos}" style="z-index: ${3 - pos}; transform: translateY(${pos * 10}px) scale(${1 - pos * 0.05});">
        ${item.imageUrl ? `<img src="${esc(item.imageUrl)}" alt="Find 이미지">` : '<div class="no-image">No Image</div>'}
        <div class="meta">
          <div class="top-row">
            <div class="writer-info">
              <img src="${esc(item.writerPhoto)}" alt="${esc(item.writerName)} 프로필 사진" class="writer-photo">
              <div>
                <div class="title">${esc(item.writerName || "Fin'd")}</div>
                <div class="place">📍 ${esc(item.locationName || '')}</div>
              </div>
            </div>
            ${item.isAuthor ? `<button class="del-btn" data-del>🗑</button>` : ''}
          </div>
          <div class="bottom-right">
            <div>${esc(item.createdAt || '')}</div>
            <div>${esc(item.remainingTime || '')}</div>
          </div>
        </div>
        ${showBadge ? `<div class="badge">⏰</div>` : ''}
      </article>
    `;
    }

    /**
     * 현재 아이템 목록을 기반으로 스택을 화면에 다시 그립니다.
     * @param {Array<object>} list - 전체 카드 데이터 목록
     */
    function paintStack(list) {
        stackEl.innerHTML = '';
        // 화면에는 최대 3개의 카드만 보여주어 성능을 최적화합니다.
        list.slice(0, 3).reverse().forEach((it, i) => {
            // reverse()를 사용해 z-index가 낮은 카드부터 그립니다.
            const pos = 2 - i;
            stackEl.insertAdjacentHTML('beforeend', cardTpl(it, pos));
        });
        attachInteractions(list);
    }

    /**
     * 스택에 스와이프 및 클릭 인터랙션을 추가합니다.
     * @param {Array<object>} list - 전체 카드 데이터 목록
     */
    function attachInteractions(list) {
        let startX = 0, dx = 0, dragging = false;

        const activeCard = () => stackEl.querySelector('.card[data-pos="0"]');

        const onStart = (x) => {
            const card = activeCard();
            if (!card) return;
            dragging = true;
            startX = x;
            card.classList.add('dragging');
        };

        const onMove = (x) => {
            if (!dragging) return;
            const card = activeCard();
            if (!card) return;
            dx = x - startX;
            card.style.transition = 'none';
            const rot = Math.max(-10, Math.min(10, dx / 20)); // 회전 각도 제한
            card.style.transform = `translate(${dx}px, 0) rotate(${rot}deg)`;
        };

        const onEnd = () => {
            if (!dragging) return;
            dragging = false;
            const card = activeCard();
            if (!card) return;
            card.classList.remove('dragging');

            const decisionThreshold = card.offsetWidth * 0.3; // 카드 너비의 30% 이상 스와이프해야 넘김

            if (Math.abs(dx) > decisionThreshold) {
                // 카드 넘기기
                const direction = dx > 0 ? 1 : -1;
                card.style.transition = 'transform .3s ease, opacity .3s ease';
                card.style.transform = `translate(${direction * 500}px, 0) rotate(${direction * 15}deg)`;
                card.style.opacity = '0';

                // 애니메이션이 끝난 후 스택을 재구성합니다.
                setTimeout(() => {
                    const first = list.shift();
                    list.push(first); // 넘긴 카드를 맨 뒤로 보냅니다.
                    paintStack(list);
                }, 300);
            } else {
                // 원위치로 복귀
                card.style.transition = 'transform .3s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
                card.style.transform = '';
            }
            dx = 0;
        };

        // 포인터 이벤트(터치/마우스 통합)를 사용하여 상호작용을 처리합니다.
        stackEl.onpointerdown = (e) => {
            // 삭제 버튼을 눌렀을 때는 스와이프를 시작하지 않습니다.
            if (e.target.closest('[data-del]')) return;
            e.preventDefault();
            onStart(e.clientX);
        };
        stackEl.onpointermove = (e) => {
            e.preventDefault();
            onMove(e.clientX);
        };
        // 마우스를 놓거나 터치가 끝나면 onEnd를 호출합니다.
        stackEl.onpointerup = onEnd;
        stackEl.onpointerleave = onEnd;

        // 삭제 버튼 처리
        stackEl.addEventListener('click', async (e) => {
            const btn = e.target.closest('[data-del]');
            if (!btn) return;

            const el = e.target.closest('.card');
            const id = el?.dataset.id;
            if (!id) return;

            if (!confirm('이 Fin\'d를 삭제할까요?')) return;

            try {
                const response = await fetch(`/find/${id}`, {method: 'DELETE'});
                if (response.ok) {
                    const idx = list.findIndex(x => String(x.id) === String(id));
                    if (idx >= 0) list.splice(idx, 1);

                    if (!list.length) {
                        stackEl.innerHTML = `<div style="padding:16px;color:#666;text-align:center;">Fin'd가 모두 삭제되었습니다.</div>`;
                        return;
                    }
                    paintStack(list);
                } else {
                    throw new Error('삭제에 실패했습니다.');
                }
            } catch (err) {
                alert('삭제 중 오류가 발생했습니다.');
                console.error('Delete error:', err);
            }
        });
    }

    // 4) 초기 렌더
    if (!items.length) {
        sc.innerHTML = `<div style="padding:16px;color:#666;text-align:center;">표시할 Fin'd가 없습니다.</div>`;
        return;
    }
    paintStack(items);
}

