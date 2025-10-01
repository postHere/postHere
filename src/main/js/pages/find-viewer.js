// find-viewer.js (교체본) — 서버가 렌더해 둔 DOM을 읽어 스택 뷰로 변환
export function initFindViewer() {
    console.log('[find-viewer] initFindViewer start');
    // 1) DOM에서 데이터 수집
    const sc = document.getElementById('swipe-container');
    if (!sc) return;

    const startIndex = Number(sc.getAttribute('data-start-index') || 0);

    // 렌더된 카드들을 읽어서 items 배열 생성
    // 렌더된 카드들을 읽어서 items 배열 생성
    const serverCards = Array.from(sc.querySelectorAll('.card'));
    const items = serverCards.map((el) => {
        const id = Number(el.getAttribute('data-find-id'));
        const imgEl = el.querySelector('.image-container img');
        const profileImg = el.querySelector('.writer-info img')?.getAttribute('src') || '';
        const writer = el.querySelector('.nickname')?.textContent?.trim() || '';
        const location = el.querySelector('.footer > span')?.textContent?.trim() || '';
        const timeBlock = el.querySelector('.footer > div');
        let createdAt = '', remaining = '';
        if (timeBlock) {
            const spans = timeBlock.querySelectorAll('span');
            createdAt = spans[0]?.textContent?.trim() || '';
            remaining = spans[1]?.textContent?.trim() || '';
        }
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


// 2-NEW) 최신 → 오래된 순으로 전체 정렬한 뒤, 선택한 글을 맨 앞으로 회전
// URL의 마지막 세그먼트(예: /find/viewer/123)에서 선택 ID 추출
    const pathParts = location.pathname.split('/').filter(Boolean);
    const urlId = Number(pathParts[pathParts.length - 1]);
    const selectedId = Number.isFinite(urlId) ? urlId : items[0]?.id;

// "yyyy.MM.dd HH:mm" → Date 파서 (실패하면 null)
    function parseKST(s) {
        if (!s) return null;
        // "2025.01.03 14:25" 같은 포맷 가정
        const m = String(s).match(/^(\d{4})\.(\d{2})\.(\d{2})\s+(\d{2}):(\d{2})$/);
        if (!m) return null;
        const [_, y, mo, d, h, mi] = m.map(Number);
        // 로컬 타임으로 생성
        return new Date(y, mo - 1, d, h, mi, 0, 0);
    }

// 최신(id/시간이 클수록 최신) → 오래된 순으로 정렬
    items.sort((a, b) => {
        const ta = parseKST(a.createdAt);
        const tb = parseKST(b.createdAt);
        if (ta && tb) return tb - ta;   // createdAt 있으면 시간 기준(내림차순)
        if (a.id != null && b.id != null) return b.id - a.id; // fallback: id 기준
        return 0;
    });

// 선택한 글(selectedId)이 맨 앞(0번 인덱스)에 오도록 회전
    const selIdx = items.findIndex(x => Number(x.id) === selectedId);
    if (selIdx > 0) {
        const head = items.splice(0, selIdx);
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
    // XSS 방지 이스케이프
    const esc = (s) => String(s ?? '').replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[m]));

// ✅ 전체 주소에서 '동'으로 끝나는 토큰까지 포함해 잘라서 반환
    function upToDong(addr = '') {
        const parts = String(addr).trim().split(/\s+/);
        const idx = parts.findIndex(p => /동$/.test(p));
        return idx >= 0 ? parts.slice(0, idx + 1).join(' ') : parts.join(' ');
    }

    // ✅ 프로필 이미지가 없으면 기본 이미지로 대체
    function avatarSrc(src) {
        return (src && String(src).trim() && String(src).toLowerCase() !== 'null')
            ? src
            : '/images/default-profile.png';
    }


    // [추가] 카드 템플릿 위쪽에 넣기 (initFindViewer 내부)
    function formatKoreanDate(createdAtStr) {
        if (!createdAtStr) return '';
        // 1) 서버가 "yyyy.MM.dd HH:mm"로 주는 경우
        const m = String(createdAtStr).match(/^(\d{4})\.(\d{2})\.(\d{2})/);
        if (m) {
            const y = Number(m[1]), mo = Number(m[2]), d = Number(m[3]);
            return `${y}년 ${mo}월 ${d}일`;
        }
        // 2) ISO 등 Date로 파싱 가능한 경우
        const dt = new Date(createdAtStr);
        if (!isNaN(dt)) {
            return `${dt.getFullYear()}년 ${dt.getMonth() + 1}월 ${dt.getDate()}일`;
        }
        // 3) 포맷을 못 알아보면 원본 유지
        return createdAtStr;
    }

    function formatRemaining(remStr) {
        if (!remStr) return '';
        if (/만료/.test(remStr)) return '만료';
        // "HH:MM 남음" → "n시간 후 종료"/"n분 후 종료"
        const m = remStr.match(/^(\d{1,2}):(\d{2})\s*남음$/);
        if (m) {
            const h = Number(m[1]), mi = Number(m[2]);
            if (h >= 1) return `${h}시간 후 종료`;
            return `${mi}분 후 종료`;
        }
        // 이미 "n시간 후 종료" 형태면 그대로
        if (/시간\s*후\s*종료|분\s*후\s*종료/.test(remStr)) return remStr;
        return remStr;
    }


    /**
     * 카드 1개의 HTML 템플릿을 생성합니다.
     * @param {object} item - 카드 데이터
     * @param {number} pos - 스택에서의 위치 (0: 맨 위, 1: 중간, 2: 맨 아래)
     * @returns {string} - 생성된 HTML 문자열
     */
    function cardTpl(item, pos = 0) {
        return `
    <article class="fv-card" data-id="${esc(item.id)}" data-pos="${pos}"
             style="z-index:${3 - pos}; transform: translateY(${pos * 10}px) scale(${1 - pos * 0.05});">
      ${item.imageUrl ? `<img src="${esc(item.imageUrl)}" alt="Fin'd 이미지">` : '<div class="no-image">No Image</div>'}

     <div class="fv-overlay-top">
        <img class="fv-avatar" src="${esc(avatarSrc(item.writerPhoto))}" alt="${esc(item.writerName)}">
        <div class="fv-top-text">
          <div class="fv-nickname">${esc(item.writerName || "Fin'd")}</div>
          <div class="fv-location">📍 ${esc(upToDong(item.locationName) || '')}</div>
        </div>
     </div>

      <!-- 우하단 오버레이: 작성일 • 남은시간 -->
      <div class="fv-overlay-bottom">
        <span class="fv-date">${esc(formatKoreanDate(item.createdAt))}</span>
        <span class="fv-sep">•</span>
        <span class="fv-remaining">${esc(formatRemaining(item.remainingTime))}</span>
      </div>
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
        let startY = 0;
        let activeEl = null;
        const EDGE_GUARD = 18;   // iOS 뒤로가기 제스처 영역 보호
        const SWIPE_THRESHOLD = 80;

        const active = () => stackEl.querySelector('.fv-card[data-pos="0"]');

        const begin = (x, y) => {
            const a = active();
            if (!a) return;
            // 화면 가장자리에서 시작한 스와이프는 무시(뒤로가기 제스처 보호)
            if (x < EDGE_GUARD || x > (window.innerWidth - EDGE_GUARD)) return;

            dragging = true;
            startX = x;
            startY = y;
            activeEl = a;
            a.classList.add('dragging');
        };

        const move = (x, y, isTouchMove = false, rawEvent = null) => {
            if (!dragging || !activeEl) return;

            // 세로로 크게 움직이면(스크롤 의도) 드래그 취소
            if (Math.abs(y - startY) > 12 && Math.abs(y - startY) > Math.abs(x - startX)) {
                cancel();
                return;
            }

            dx = x - startX;
            activeEl.style.transition = 'none';
            const rot = Math.max(-10, Math.min(10, dx / 10));
            activeEl.style.transform = `translate(${dx}px, 0) rotate(${rot}deg)`;

            // 터치 스크롤을 막아 주어야 스와이프가 끊기지 않음
            if (isTouchMove && rawEvent) rawEvent.preventDefault();
        };

        const end = () => {
            if (!dragging || !activeEl) return;
            const a = activeEl;

            dragging = false;
            activeEl = null;

            a.classList.remove('dragging');
            a.style.transition = 'transform .28s ease, opacity .28s ease';

            if (Math.abs(dx) > SWIPE_THRESHOLD) {
                a.style.transform = `translate(${dx > 0 ? 480 : -480}px, 0) rotate(${dx > 0 ? 15 : -15}deg)`;
                a.style.opacity = '0';
                setTimeout(() => {
                    const first = list.shift();
                    list.push(first);
                    paintStack(list);
                }, 250);
            } else {
                a.style.transform = '';
            }
            dx = 0;
        };

        const cancel = () => {
            dragging = false;
            if (activeEl) {
                activeEl.classList.remove('dragging');
                activeEl.style.transform = '';
                activeEl.style.transition = '';
            }
            activeEl = null;
            dx = 0;
        };

        // ----- Pointer Events (지원 브라우저/웹뷰용) -----
        stackEl.addEventListener('pointerdown', (e) => begin(e.clientX, e.clientY));
        stackEl.addEventListener('pointermove', (e) => move(e.clientX, e.clientY));
        window.addEventListener('pointerup', end);
        window.addEventListener('pointercancel', cancel);

        // ----- Touch Fallback (iOS WebView 등) -----
        stackEl.addEventListener('touchstart', (e) => {
            const t = e.touches[0];
            if (!t) return;
            begin(t.clientX, t.clientY);
        }, {passive: true});

        stackEl.addEventListener('touchmove', (e) => {
            const t = e.touches[0];
            if (!t) return;
            // passive:false 여야 preventDefault가 동작합니다.
        }, {passive: false});

        // 실제 move 처리 (passive:false로 다시 등록)
        stackEl.addEventListener('touchmove', (e) => {
            const t = e.touches[0];
            if (!t) return;
            move(t.clientX, t.clientY, true, e); // isTouchMove=true
        }, {passive: false});

        window.addEventListener('touchend', end, {passive: true});
        window.addEventListener('touchcancel', cancel, {passive: true});


    }

    // 4) 초기 렌더
    if (!items.length) {
        sc.innerHTML = `<div style="padding:16px;color:#666;text-align:center;">표시할 Fin'd가 없습니다.</div>`;
        return;
    }
    paintStack(items);
}

