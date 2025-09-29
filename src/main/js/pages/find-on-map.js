/**
 * find-on-map.js
 *
 * [변경 요약]
 * - ❗ 모달 미표시 원인 해결:
 *     · CardOverlay를 overlayMouseTarget → floatPane 으로 변경(마커 위 확실히 노출).
 *     · onAdd()/set()에서 projection 준비 전이면 map 'idle'에 draw 예약.
 * - ❗ 리스트 갱신 실패시 안전: /around-finds 실패면 기존 마커/선택/모달을 유지(절대 clear하지 않음).
 * - ❗ 진단 로그 추가: 마커 수/선택/오버레이 draw/네트워크 응답 상태를 상세 로깅.
 * - 최초 빠른 센터링(primeCenterFast) 유지 + 위치/네트워크 로그 강화.
 * - 자동 센터링 UX(제스처 쿨다운·오프스크린·100m 이동 기준) 및 선택 고정 로직은 유지.
 */

import {Geolocation} from '@capacitor/geolocation';
import {authHeaders} from '../modules/utils.js';

/* ===== 색상/반경 ===== */
const GREEN_DEEP = '#2f5b3d';
const R_SMALL = 50;   // 50m
const R_BIG = 200;  // 200m

/* ===== 마커 리소스 ===== */
const ICON_SRC = {
    outline: '/images/marker_region_2.png', // 선택 전(외곽선)
    filled: '/images/marker_region_1.png'  // 선택 후(채움)
};
const MARKER_SIZE = {w: 36, h: 36};
const MARKER_ANCHOR = {x: 18, y: 36}; // 하단 중앙(anchor)

/* 내부 상태 */
let map, geocoder, overlay;
let didFitZoom = false;
let bootHasRun = false;
let geoWatchId = null;

let markers = [];               // { marker, data, isSelected, dotOverlay? }
let infoOverlay = null;         // 카드 오버레이 인스턴스
let selectedMarkerRef = null;

let selectedFindId = null;      // 현재 핀(선택)된 fin'd id
let selectionPinned = false;    // 사용자가 직접 선택한 상태인지(자동 해제 금지)

let currentPos = null;          // {lat, lng}
let firstFixDone = false;       // 첫 위치 수신 여부

/* 자동 센터링 파라미터 */
const GESTURE_COOLDOWN_MS = 5000;
const AUTOCENTER_DISTANCE_M = 100;
const VIEWPORT_PADDING_PX = 24;
let lastUserGestureAt = 0;
let lastAutoCenterAt = 0;
let lastAutoCenterCoord = null;
let isProgrammaticPan = false;

/* 틴팅 캐시 */
const TINT_CACHE = {outline: null, filled: null};

/* ───────────────────────── 유틸 ───────────────────────── */

function zoomForRadiusMeters(meters, lat, frac) {
    const pxRadius = Math.min(window.innerWidth, window.innerHeight) * frac;
    const mppDesired = meters / pxRadius;
    const denom = 156543.03392 * Math.cos(lat * Math.PI / 180);
    const z = Math.log2(denom / mppDesired);
    return Math.max(3, Math.min(21, z));
}

function metersToPixels(meters, zoom, lat) {
    const mpp = 156543.03392 * Math.cos(lat * Math.PI / 180) / Math.pow(2, zoom);
    return meters / mpp;
}

function haversineMeters(a, b) {
    if (!a || !b) return Infinity;
    const toRad = (d) => d * Math.PI / 180;
    const R = 6371000;
    const dLat = toRad(b.lat - a.lat);
    const dLng = toRad(b.lng - a.lng);
    const lat1 = toRad(a.lat);
    const lat2 = toRad(b.lat);
    const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(h));
}

function composeShortAddress(result) {
    if (!result) return null;
    const comps = result.address_components || [];
    const pick = (t) => (comps.find(c => c.types.includes(t)) || {}).long_name || null;
    const dong = pick('sublocality_level_2') || pick('sublocality_level_1') || pick('neighborhood');
    const route = pick('route');
    const streetNo = pick('street_number');
    const parts = [];
    if (dong) parts.push(dong);
    if (route) parts.push(streetNo ? `${route} ${streetNo}` : route);
    return parts.length ? parts.join(' · ') : (result.formatted_address || null);
}

/* PNG 전체를 초록색으로 틴팅하되 알파는 유지 */
function tintToGreen(url, color = GREEN_DEEP) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.onload = () => {
            const c = document.createElement('canvas');
            c.width = img.width;
            c.height = img.height;
            const ctx = c.getContext('2d');
            ctx.drawImage(img, 0, 0);
            ctx.globalCompositeOperation = 'source-in';
            ctx.fillStyle = color;
            ctx.fillRect(0, 0, c.width, c.height);
            resolve(c.toDataURL('image/png'));
        };
        img.onerror = reject;
        img.src = url;
    });
}

async function prepareTintedIcons() {
    if (!TINT_CACHE.outline) TINT_CACHE.outline = await tintToGreen(ICON_SRC.outline);
    if (!TINT_CACHE.filled) TINT_CACHE.filled = await tintToGreen(ICON_SRC.filled);
}

/* window.google 참조는 함수 내부에서만 */
function markerIcon(kind /* 'outline' | 'filled' */) {
    const g = window.google;
    const url = (kind === 'filled') ? (TINT_CACHE.filled || ICON_SRC.filled)
        : (TINT_CACHE.outline || ICON_SRC.outline);
    return {
        url,
        scaledSize: new g.maps.Size(MARKER_SIZE.w, MARKER_SIZE.h),
        anchor: new g.maps.Point(MARKER_ANCHOR.x, MARKER_ANCHOR.y),
        labelOrigin: new g.maps.Point(MARKER_ANCHOR.x, 0)
    };
}

/* ───────────────────────── Overlays: Center/Dot/Card ───────────────────────── */
let CenterOverlayClass = null;

function getCenterOverlayClass() {
    const g = window.google;
    if (!(g && g.maps && g.maps.OverlayView)) return null;
    if (CenterOverlayClass) return CenterOverlayClass;

    class CenterOverlay extends g.maps.OverlayView {
        constructor(centerLatLng, mapInstance) {
            super();
            this.center = centerLatLng;
            this.map = mapInstance;
            this.container = document.createElement('div');
            this.container.id = 'dom-center-overlay';
            this.r200 = document.createElement('div');
            this.r200.className = 'o-item o-ring-200';
            this.r50 = document.createElement('div');
            this.r50.className = 'o-item o-ring-50';
            this.dot = document.createElement('div');
            this.dot.className = 'o-item o-user-dot';
            this.container.appendChild(this.r200);
            this.container.appendChild(this.r50);
            this.container.appendChild(this.dot);
            this.setMap(mapInstance);
        }

        onAdd() {
            this.getPanes().overlayLayer.appendChild(this.container);
        }

        onRemove() {
            if (this.container.parentNode) this.container.parentNode.removeChild(this.container);
        }

        setCenter(latLng) {
            this.center = latLng;
            this.draw();
        }

        draw() {
            if (!this.center) return;
            const proj = this.getProjection();
            if (!proj) return;
            const pt = proj.fromLatLngToDivPixel(this.center);
            const z = this.map.getZoom();
            const lat = this.center.lat();
            const r50px = metersToPixels(R_SMALL, z, lat);
            const r200px = metersToPixels(R_BIG, z, lat);
            this.container.style.left = `${pt.x}px`;
            this.container.style.top = `${pt.y}px`;
            this.container.style.transform = 'translate(-50%, -50%)';
            this.r50.style.width = `${r50px * 2}px`;
            this.r50.style.height = `${r50px * 2}px`;
            this.r200.style.width = `${r200px * 2}px`;
            this.r200.style.height = `${r200px * 2}px`;
        }
    }

    CenterOverlayClass = CenterOverlay;
    return CenterOverlayClass;
}

let DotOverlayClass = null;

function getDotOverlayClass() {
    const g = window.google;
    if (!(g && g.maps && g.maps.OverlayView)) return null;
    if (DotOverlayClass) return DotOverlayClass;

    class DotOverlay extends g.maps.OverlayView {
        constructor(pos) {
            super();
            this.pos = pos;
            this.div = document.createElement('div');
            this.div.className = 'dot-badge';
            this.div.style.position = 'absolute';
        }

        onAdd() {
            this.getPanes().overlayMouseTarget.appendChild(this.div);
        }

        onRemove() {
            if (this.div && this.div.parentNode) this.div.parentNode.removeChild(this.div);
        }

        setPosition(pos) {
            this.pos = pos;
            this.draw();
        }

        draw() {
            if (!this.pos) return;
            const proj = this.getProjection();
            if (!proj) return;
            const pt = proj.fromLatLngToDivPixel(this.pos);
            const iconTop = pt.y - MARKER_ANCHOR.y;
            const iconLeft = pt.x - MARKER_ANCHOR.x;
            const margin = 2, dotSize = 12;
            const left = iconLeft + (MARKER_SIZE.w - dotSize - margin);
            const top = iconTop + margin;
            this.div.style.left = `${left}px`;
            this.div.style.top = `${top}px`;
        }
    }

    DotOverlayClass = DotOverlay;
    return DotOverlayClass;
}

let CardOverlayClass = null;

function getCardOverlayClass() {
    const g = window.google;
    if (!(g && g.maps && g.maps.OverlayView)) return null;
    if (CardOverlayClass) return CardOverlayClass;

    class CardOverlay extends g.maps.OverlayView {
        constructor() {
            super();
            this.pos = null;
            this._root = document.createElement('div');
            this._root.style.position = 'absolute';
            this._root.style.zIndex = '25';
            this._root.style.pointerEvents = 'auto';
            this._root.style.transform = 'translate(-50%, 0)';
            this._root.style.willChange = 'left, top';
            this._root.style.userSelect = 'none';
            this._root.style.webkitUserSelect = 'none';
        }

        onAdd() {
            // ▼▼▼ [변경] 카드 레이어를 floatPane으로 올려 마커/레이블 위로 보장
            this.getPanes().floatPane.appendChild(this._root);
            // pos가 설정된 상태에서 onAdd가 뒤늦게 오면 idle에서 draw 보장
            if (this.pos) g.maps.event.addListenerOnce(this.getMap(), 'idle', () => this.draw());
        }

        onRemove() {
            if (this._root.parentNode) this._root.parentNode.removeChild(this._root);
        }

        set(position, html) {
            this.pos = position;
            this._root.innerHTML = html;
            // projection 미준비면 idle에서 1회 draw 예약
            if (this.getProjection()) {
                this.draw();
            } else {
                const m = this.getMap();
                if (m) g.maps.event.addListenerOnce(m, 'idle', () => this.draw());
            }
        }

        close() {
            this._root.innerHTML = '';
            this.pos = null;
            this.draw();
        }

        draw() {
            if (!this.pos) {
                this._root.style.left = '-9999px';
                this._root.style.top = '-9999px';
                return;
            }
            const proj = this.getProjection();
            if (!proj) return;
            const pt = proj.fromLatLngToDivPixel(this.pos);
            const topOfIcon = pt.y - MARKER_ANCHOR.y;
            const gap = 8;
            this._root.style.left = `${pt.x}px`;
            this._root.style.top = `${topOfIcon - gap}px`;
            console.debug('[find-on-map] card.draw at', this._root.style.left, this._root.style.top);
        }
    }

    CardOverlayClass = CardOverlay;
    return CardOverlayClass;
}

/* ───────────────────────── UI 조각 ───────────────────────── */
function buildCardHTML(item, canEnter) {
    const avatar = item.profile_image_url || '';
    const nick = item.nickname || '(알 수 없음)';
    return `
    <div class="info-card" data-nickname="${encodeURIComponent(nick)}">
      <img class="avatar" alt="Profile" src="${avatar}">
      <div class="nickname">${nick}</div>
      <div class="go-wrap">
        <button class="go-btn" ${canEnter ? '' : 'disabled aria-disabled="true"'} title="Fin'd로 이동">&raquo;&raquo;</button>
      </div>
    </div>
  `;
}

/* 선택 공통 처리 */
function applySelection(obj, {pinSelection}) {
    const g = window.google;

    selectedFindId = obj.data.find_pk || null;
    selectionPinned = !!pinSelection;

    // 기존 선택 해제(화면상의 레퍼런스만)
    if (selectedMarkerRef && selectedMarkerRef !== obj) {
        selectedMarkerRef.marker.setIcon(markerIcon('outline'));
        if (selectedMarkerRef.dotOverlay) {
            selectedMarkerRef.dotOverlay.setMap(null);
            selectedMarkerRef.dotOverlay = null;
        }
        selectedMarkerRef.isSelected = false;
    }

    // 현재 선택 비주얼
    obj.isSelected = true;
    obj.marker.setIcon(markerIcon('filled'));
    selectedMarkerRef = obj;

    // 빨간 점
    const DotCls = getDotOverlayClass();
    if (DotCls) {
        if (obj.dotOverlay) obj.dotOverlay.setMap(null);
        obj.dotOverlay = new DotCls(obj.marker.getPosition());
        obj.dotOverlay.setMap(map);
    }

    // 버튼 활성: 서버 region==="1" && 실제 거리<=50m
    let canEnter = String(obj.data.region || '2') === '1';
    if (currentPos) {
        const dist = haversineMeters(currentPos, {lat: Number(obj.data.lat), lng: Number(obj.data.lng)});
        if (!(dist <= R_SMALL)) canEnter = false;
    }

    // 카드 표시
    if (!infoOverlay) {
        const CardCls = getCardOverlayClass();
        infoOverlay = new CardCls();
        infoOverlay.setMap(map);
    }
    infoOverlay.set(obj.marker.getPosition(), buildCardHTML(obj.data, canEnter));

    // 카드 내부 이벤트 바인딩
    const root = infoOverlay._root;
    if (root) {
        const goBtn = root.querySelector('.go-btn');
        if (goBtn) {
            goBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (goBtn.hasAttribute('disabled')) return;
                const id = obj.data.find_pk;
                if (id) location.assign(`/find/${id}`);
            };
        }
        root.onclick = (e) => {
            if (e.target.closest('.go-btn')) return;
            const nick = obj.data.nickname;
            if (nick) location.assign(`/profile/${encodeURIComponent(nick)}`);
        };
    }

    console.debug('[find-on-map] selected:', {
        id: selectedFindId, pin: selectionPinned, canEnter, pos: obj.marker.getPosition()?.toUrlValue()
    });
}

/* 사용자 해제(핀 해제) */
function userDeselect() {
    selectedFindId = null;
    selectionPinned = false;
    deselectVisualOnly();
}

/* 비주얼만 해제(선택ID/핀은 유지) */
function deselectVisualOnly() {
    if (selectedMarkerRef) {
        selectedMarkerRef.marker.setIcon(markerIcon('outline'));
        if (selectedMarkerRef.dotOverlay) {
            selectedMarkerRef.dotOverlay.setMap(null);
            selectedMarkerRef.dotOverlay = null;
        }
        selectedMarkerRef.isSelected = false;
        selectedMarkerRef = null;
    }
    if (infoOverlay) infoOverlay.close();
}

/* ───────────────────────── 주소 칩 ───────────────────────── */
function updateAddressChip(latLng) {
    const g = window.google;
    if (!(g && g.maps)) return;
    if (!geocoder) geocoder = new g.maps.Geocoder();
    geocoder.geocode({location: latLng}, (results, status) => {
        const chip = document.getElementById('address-chip');
        if (!chip) return;
        if (status === 'OK' && results && results.length) {
            const addr = composeShortAddress(results[0]) || composeShortAddress(results[1]);
            chip.textContent = addr || '주소를 확인할 수 없어요';
        } else chip.textContent = '주소를 확인할 수 없어요';
    });
}

/* ───────────────────────── 서버 연동 & 마커 렌더 ───────────────────────── */
let lastFetchAt = 0;
let lastFetchCoord = null;
const FETCH_MIN_INTERVAL = 8000;
const FETCH_MIN_MOVE = 20;

let mapClickListener = null;

async function fetchAroundFinds(lat, lng) {
    try {
        const res = await fetch('/around-finds', {
            method: 'POST',
            credentials: 'include',
            headers: authHeaders({
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }),
            body: JSON.stringify({lat, lng})
        });
        const ct = res.headers.get('content-type') || '';
        console.debug('[find-on-map] around-finds status:', res.status, 'ctype:', ct);
        if (!res.ok || !ct.includes('application/json')) {
            return {ok: false, list: null, status: res.status, ctype: ct};
        }
        const json = await res.json();
        const list = Array.isArray(json?.data) ? json.data : [];
        return {ok: true, list};
    } catch (e) {
        console.error('[find-on-map] around-finds network error:', e);
        return {ok: false, list: null, error: e?.message || String(e)};
    }
}

/* 기존 마커 모두 제거(성공시에만 호출) */
function clearMarkers() {
    markers.forEach(m => {
        if (m.dotOverlay) {
            m.dotOverlay.setMap(null);
            m.dotOverlay = null;
        }
        m.marker.setMap(null);
    });
    markers = [];
    // 선택 비주얼은 다음 렌더에서 재복원할 것이므로 닫아둠
    deselectVisualOnly();
}

function renderMarkers(list) {
    const g = window.google;
    if (!(g && g.maps) || !map) return;

    clearMarkers();

    let toReSelect = null;

    list.forEach(item => {
        const lat = Number(item.lat), lng = Number(item.lng);
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

        const pos = new g.maps.LatLng(lat, lng);
        const marker = new g.maps.Marker({
            position: pos,
            map,
            clickable: true,
            icon: markerIcon('outline'),
            zIndex: 10
        });

        const obj = {marker, data: item, isSelected: false, dotOverlay: null};
        markers.push(obj);

        if (selectionPinned && selectedFindId && String(selectedFindId) === String(item.find_pk)) {
            toReSelect = obj;
        }

        marker.addListener('click', () => {
            if (obj.isSelected && selectionPinned) {
                userDeselect();
                return;
            }
            applySelection(obj, {pinSelection: true});
        });
    });

    if (toReSelect) {
        applySelection(toReSelect, {pinSelection: true});
    }

    if (mapClickListener) {
        mapClickListener.remove();
        mapClickListener = null;
    }
    mapClickListener = map.addListener('click', () => userDeselect());

    console.debug('[find-on-map] markers rendered:', markers.length);
}

async function maybeRefreshAroundFinds(lat, lng) {
    const now = Date.now();
    const elapsed = now - lastFetchAt;
    const moved = haversineMeters(lastFetchCoord, {lat, lng});
    if (elapsed < FETCH_MIN_INTERVAL && moved < FETCH_MIN_MOVE) return;

    const {ok, list, status, ctype, error} = await fetchAroundFinds(lat, lng);
    if (!ok) {
        console.warn('[find-on-map] skip rerender (keep old markers). reason=',
            error || `bad response status=${status} ctype=${ctype}`);
        // 실패 시 마커/선택/모달 유지
        return;
    }

    // 성공시에만 리렌더
    await prepareTintedIcons(); // 안전상 1회 보장
    renderMarkers(list);

    lastFetchAt = now;
    lastFetchCoord = {lat, lng};
}

/* ───────────────────────── 자동 센터링 판단 ───────────────────────── */
function isInsidePaddedViewport(lat, lng, padPx) {
    const g = window.google;
    if (!overlay) return true;
    const proj = overlay.getProjection();
    if (!proj) return true;
    const pt = proj.fromLatLngToDivPixel(new g.maps.LatLng(lat, lng));
    const mapDiv = map.getDiv();
    const w = mapDiv.clientWidth;
    const h = mapDiv.clientHeight;
    return pt.x >= padPx && pt.x <= (w - padPx) && pt.y >= padPx && pt.y <= (h - padPx);
}

function shouldAutoCenter(lat, lng) {
    if (selectionPinned) return false;
    const now = Date.now();
    if (now - lastUserGestureAt < GESTURE_COOLDOWN_MS) return false;
    const inside = isInsidePaddedViewport(lat, lng, VIEWPORT_PADDING_PX);
    if (!inside) return true;
    if (lastAutoCenterCoord) {
        const moved = haversineMeters(lastAutoCenterCoord, {lat, lng});
        if (moved >= AUTOCENTER_DISTANCE_M) return true;
    } else {
        return true;
    }
    return false;
}

/* ───────────────────────── 위치/지도 초기화 ───────────────────────── */
function onPosition(lat, lng, opts = {}) {
    currentPos = {lat, lng};
    const g = window.google;
    if (!(g && g.maps) || !map || !overlay) return;

    const center = new g.maps.LatLng(lat, lng);

    if (!firstFixDone || opts.forceCenter) {
        const z = zoomForRadiusMeters(R_BIG, lat, 0.45);
        map.setZoom(Math.round(z));
        map.setCenter(center);
        didFitZoom = true;
        firstFixDone = true;
        lastAutoCenterCoord = {lat, lng};
        console.debug('[find-on-map] first fix center');
    } else {
        if (shouldAutoCenter(lat, lng)) {
            isProgrammaticPan = true;
            map.panTo(center);
            setTimeout(() => {
                isProgrammaticPan = false;
            }, 300);
            lastAutoCenterAt = Date.now();
            lastAutoCenterCoord = {lat, lng};
            console.debug('[find-on-map] auto center(panTo)');
        }
    }

    overlay.setCenter(center);
    updateAddressChip(center);
    void maybeRefreshAroundFinds(lat, lng);
}

async function startWatchingPosition() {
    try {
        geoWatchId = await Geolocation.watchPosition(
            {enableHighAccuracy: true, timeout: 10000, maximumAge: 1000},
            (pos, err) => {
                if (err) {
                    console.warn('[find-on-map] geolocation error(capacitor)', err);
                    const chip = document.getElementById('address-chip');
                    if (chip) chip.textContent = '위치 권한이 필요해요';
                    return;
                }
                if (!pos || !pos.coords) return;
                const c = pos.coords;
                onPosition(c.latitude, c.longitude);
            }
        );
        return;
    } catch (e) {
        console.warn('[find-on-map] geolocation plugin failed, fallback to navigator', e);
    }

    if (!navigator.geolocation) {
        const chip = document.getElementById('address-chip');
        if (chip) chip.textContent = '이 기기에서 위치를 사용할 수 없어요';
        return;
    }
    navigator.geolocation.watchPosition(
        (pos) => {
            const c = pos.coords || {};
            onPosition(c.latitude, c.longitude);
        },
        (err) => {
            console.warn('[find-on-map] geolocation error(navigator)', err?.code, err?.message);
            const chip = document.getElementById('address-chip');
            if (chip) chip.textContent = '위치 권한이 필요해요';
        },
        {enableHighAccuracy: true, maximumAge: 1000, timeout: 10000}
    );
}

/* 빠른 1차 센터링(캐시 우선) */
async function primeCenterFast() {
    try {
        const pos = await Geolocation.getCurrentPosition({
            enableHighAccuracy: false, timeout: 3000, maximumAge: 60000
        });
        if (pos?.coords) {
            onPosition(pos.coords.latitude, pos.coords.longitude, {forceCenter: true});
            return;
        }
    } catch (e) {
        try {
            await new Promise((resolve, reject) => {
                if (!navigator.geolocation) return reject(new Error('no navigator.geolocation'));
                navigator.geolocation.getCurrentPosition(
                    (p) => {
                        onPosition(p.coords.latitude, p.coords.longitude, {forceCenter: true});
                        resolve();
                    },
                    reject,
                    {enableHighAccuracy: false, timeout: 2000, maximumAge: 300000}
                );
            });
            return;
        } catch {
        }
    }
    // 실패 시 watchPosition 첫 fix로 대체
}

async function initMap() {
    const g = window.google;
    const GREY_STYLE = [
        {elementType: 'geometry', stylers: [{color: '#eff2f3'}]},
        {elementType: 'labels.icon', stylers: [{visibility: 'off'}]},
        {elementType: 'labels.text.fill', stylers: [{color: '#7b7f83'}]},
        {elementType: 'labels.text.stroke', stylers: [{color: '#eff2f3'}]},
        {featureType: 'poi', stylers: [{visibility: 'off'}]},
        {featureType: 'road', elementType: 'geometry', stylers: [{color: '#dfe4e7'}]},
        {featureType: 'road', elementType: 'labels', stylers: [{visibility: 'off'}]},
        {featureType: 'water', elementType: 'geometry', stylers: [{color: '#eef2f4'}]}
    ];

    const fallback = {lat: 37.5662952, lng: 126.9779451};
    map = new g.maps.Map(document.getElementById('map'), {
        center: fallback,
        zoom: 16,
        disableDefaultUI: true,
        clickableIcons: false,
        gestureHandling: 'greedy',
        styles: GREY_STYLE
    });

    const OverlayCls = getCenterOverlayClass();
    overlay = new OverlayCls(new g.maps.LatLng(fallback.lat, fallback.lng), map);

    // 제스처 추적
    map.addListener('dragstart', () => {
        if (!isProgrammaticPan) lastUserGestureAt = Date.now();
    });
    map.addListener('zoom_changed', () => {
        if (!isProgrammaticPan) lastUserGestureAt = Date.now();
        overlay && overlay.draw();
    });
    map.addListener('dragend', () => overlay && overlay.draw());

    // 빠른 1차 센터링 시도(대기 X)
    primeCenterFast();
    // 위치 추적 시작
    startWatchingPosition();
    // 틴팅 준비(대기 X)
    void prepareTintedIcons();

    // 원 크기 보정
    map.addListener('idle', () => overlay && overlay.draw());
}

/* ------- 부트스트랩 ------- */
function boot() {
    if (bootHasRun) return;
    const isThisPage = document.body && document.body.id === 'page-on-map';
    if (!isThisPage) return;
    const g = window.google;
    if (!(g && g.maps)) return;
    bootHasRun = true;
    initMap();
}

/* ---- 전역 브리지 ---- */
if (typeof window !== 'undefined') {
    window.__initFindOnMap = boot;
    if (!window.initFindOnMap) window.initFindOnMap = () => boot();
}

/* ====== 페이지 초기화 엔트리 ====== */
export function initFindOnMap() {
    if (!(document && document.body && document.body.id === 'page-on-map')) return;
    const g = window.google;
    if (g && g.maps) boot();
}
