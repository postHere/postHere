/**
 * app.js
 * 우리 애플리케이션의 모든 JavaScript 코드의 시작점(Entry Point)이자,
 * 각 페이지에 필요한 스크립트를 연결해주는 사령탑(Control Tower) 역할을 합니다.
 */

// --- 1. 전역 기능 모듈 Import ---
import './modules/pwa.js';
import {App} from '@capacitor/app';

// ✅ [PUSH] 서비스워커 라우팅/구독 유틸 추가
import {ensurePushSubscription, listenServiceWorkerNavigate} from './modules/push-subscription.js';

// --- 2. 페이지별 기능 모듈 Import ---
import {initMainNav} from './modules/main-nav.js';
import {initMain} from './pages/main.js';
import {initLogin} from './pages/login.js';
import {initSignup} from './pages/signup.js';
import {initFriends} from './pages/friends.js';
import {initProfile} from './pages/profile.js';
import {initFindWrite} from './pages/find-write.js';
import {initForumWrite} from './pages/forum-write.js';
import {initForumAreaSearch} from './pages/forum-area-search.js';
import {initParkWrite} from './pages/park-write.js';
import {initNotification} from './pages/notification.js';
import {initFindOnMap} from "./pages/find-on-map";

// --- 3. 초기 경로 설정 ---
if (window.location.pathname === '/') {
    window.location.replace('/start');
}

// --- 4. 전역 기능 실행 (뒤로 가기 버튼 처리) ---
App.addListener('backButton', ({canGoBack}) => {
    if (canGoBack) {
        window.history.back();
        return;
    }
    const currentPage = window.location.pathname;
    const exitPages = ['/login', '/start', '/forumMain'];
    if (exitPages.includes(currentPage)) {
        void App.exitApp();
    } else {
        window.location.href = '/forumMain';
    }
});

// ✅ [PUSH] SW → 클라이언트 라우팅 메시지 리스너는 항상 켜둠
listenServiceWorkerNavigate();

// ✅ [PUSH] 푸시 구독은 로그인/회원가입 페이지가 아닐 때만 시도 (401 회피)
(async () => {
    try {
        const path = window.location.pathname;
        const isAuthPage = path === '/login' || path === '/signup';
        if (!isAuthPage) {
            await ensurePushSubscription(); // 서비스워커 등록(또는 ready), 권한, 구독 생성, 서버 저장
        }
    } catch (e) {
        console.error('push init failed', e);
    }
})();

// --- 5. 페이지 라우터 ---
document.addEventListener('DOMContentLoaded', () => {
    const pageId = document.body.id;
    console.log(`현재 페이지 ID: ${pageId}. 해당 스크립트를 초기화합니다.`);

    initMainNav();

    switch (pageId) {
        case 'page-main':
            initMain();
            break;
        case 'page-login':
            initLogin();
            break;
        case 'page-signup':
            initSignup();
            break;
        case 'page-friends':
            initFriends();
            break;
        case 'page-profile':
            initProfile();
            break;
        case 'page-find-write':
            initFindWrite();
            break;
        case 'page-on-map' :
            initFindOnMap();
            break
        case 'page-forum-write':
            initForumWrite();
            break;
        case 'page-forum-area-search':
            initForumAreaSearch();
            break;
        case 'page-park-write':
            initParkWrite();
            break;
        case 'page-notifications':
            initNotification();
            break;
        default:
            console.log('이 페이지에 해당하는 초기화 스크립트가 없습니다.');
            break;
    }
});
