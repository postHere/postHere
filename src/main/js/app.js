/**
 * app.js
 * 우리 애플리케이션의 모든 JavaScript 코드의 시작점(Entry Point)이자,
 * 각 페이지에 필요한 스크립트를 연결해주는 사령탑(Control Tower) 역할을 합니다.
 */

// --- 1. 전역 기능 모듈 Import ---
// 앱 전체에 걸쳐 필요한 기능들을 불러옵니다.
// PWA 서비스 워커 등록 및 푸시 알림 설정을 시작합니다.
import './modules/pwa.js';
// Capacitor의 네이티브 기능을 사용하기 위해 App 플러그인을 불러옵니다.
import {App} from '@capacitor/app';


// --- 2. 페이지별 기능 모듈 Import ---
// 각 페이지에서 사용할 init 함수들을 불러옵니다.
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


// --- 3. 전역 기능 실행 ---
// Capacitor의 하드웨어 뒤로 가기 버튼 이벤트를 처리합니다.
// 이 코드는 페이지와 상관없이 앱 전체에서 항상 동작해야 합니다.
App.addListener('backButton', ({canGoBack}) => {
    const currentPage = window.location.pathname;
    // 뒤로 갈 페이지가 없고, 현재 페이지가 메인 페이지일 때만 앱을 종료합니다.
    if (!canGoBack && (currentPage === '/forumMain' || currentPage === '/')) {
        App.exitApp();
    } else {
        window.history.back();
    }
});


// --- 4. 페이지 라우터: 현재 페이지에 맞는 스크립트 실행 ---
// HTML 문서의 로딩이 완료되면, 현재 페이지를 확인하고 그에 맞는 init 함수를 실행합니다.
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
