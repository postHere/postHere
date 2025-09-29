import {Preferences} from '@capacitor/preferences';
import BackgroundGeolocation from '@transistorsoft/capacitor-background-geolocation';

const serverConfig = require('../../../../SERVER_URL.js');

let isInitialized = false;
const url = serverConfig.url + '/location';


export async function initBackgroundGeolocation() {

    if (isInitialized) {
        console.log('위치 추적기가 이미 존재');
        return;
    }

    console.log('위치 추적기 초기화 시작');

    //위치 로그용 이벤트 (없어도 기능에는 영향 없음)
    BackgroundGeolocation.onLocation(location => {
            console.log('[location]', location.coords.latitude, location.coords.longitude,);
        },
        error => {
            console.log('[location]', error)
        });


    //네이티브 HTTP 요청이 성공/실패할 때마다 호출됩니다.
    BackgroundGeolocation.onHttp(async (response) => {

        console.log('[http] success: ', response.responseText);

        // 서버 응답을 Preferences에 저장
        try {
            const wrapper = JSON.parse(response.responseText);
            const data = wrapper.data;

            if (wrapper.status === '000' && data.forumKey && data.forumName) {
                await Preferences.set(
                    {
                        key: 'currentAreaKey',
                        value: data.forumKey
                    });
                await Preferences.set(
                    {
                        key: 'currentAreaName',
                        value: data.forumName
                    });
                console.log('[http] 새로운 포럼 정보 저장 완료:', data.forumKey, data.forumName);

                const event = new CustomEvent('locationUpdated', {
                    detail: {
                        areaKey: data.forumKey,
                        areaName: data.forumName
                    }
                });
                window.dispatchEvent(event);
            }
        } catch (e) {
            console.error("서버 응답 파싱 실패", e);
        }
    }, error => {
        console.error('[http] failure: ', error);
    });

    try {

        const {value: user} = await Preferences.get({key: 'user'});
        console.log('location : ', user);

        // 3. ready() 메소드로 플러그인의 모든 설정을 한 번에 구성합니다.
        const state = await BackgroundGeolocation.ready({

            // 위치 추적 설정
            desiredAccuracy: BackgroundGeolocation.DESIRED_ACCURACY_HIGH,
            distanceFilter: 10000,
            elasticityMultiplier: 2.0,

            // 네이티브 HTTP 설정
            autoSync: true, //위치를 감지할 때마다 아래 url로 위치 전송
            url: url,
            method: 'POST',
            locationTemplate: '{"latitude":<%= latitude %>,"longitude":<%= longitude %>}',
            params: {
                "user": user,
            },
            headers: {
                'Content-Type': 'application/json'
            },
            // 앱 생명주기 설정
            stopOnTerminate: false, // 앱이 강제 종료되어도 추적을 멈추지 않음 (필수)
            startOnBoot: true,     // 휴대폰 재부팅 시 자동으로 추적 시작 (필수)

            backgroundPermissionRationale: {
                title: "백그라운드 위치 정보 권한이 필요합니다",
                message: "정확한 서비스 제공을 위해 위치 권한을 '항상 허용'으로 설정해주세요.",
                positiveAction: "설정으로 이동"
            },

            // 알림 설정
            notification: {
                title: "postHere 실행 중",
                text: "위치 정보를 사용하여 주변 게시물을 확인하고 있습니다.",
                sticky: true
            },

            // 디버깅 설정
            logLevel: BackgroundGeolocation.LOG_LEVEL_VERBOSE,
            debug: false // 개발 중에는 true로 설정하여 로그 확인
        })

        const permission = await BackgroundGeolocation.requestPermission();

        let shouldStart = true;

        switch (permission) {
            case BackgroundGeolocation.AUTHORIZATION_STATUS_ALWAYS:
                console.log("[Geolocation] 권한: 항상 허용.");
                break;

            case BackgroundGeolocation.AUTHORIZATION_STATUS_WHEN_IN_USE:
                console.log("[Geolocation] 권한: 앱 사용 중에만 허용. 설정 변경이 필요합니다.");
                break;

            default:
                console.log("[Geolocation] 권한 없음. 새로 요청합니다.");
                const newStatus = await BackgroundGeolocation.requestPermission();
                if (newStatus === BackgroundGeolocation.AUTHORIZATION_STATUS_ALWAYS) {
                    console.log("[Geolocation] 권한이 '항상 허용'으로 부여되었습니다.");
                } else {
                    console.log("[Geolocation] '항상 허용' 권한을 얻지 못했습니다.");
                    // '항상 허용'이 아니면 시작하지 않도록 설정
                    shouldStart = false;
                }
                break;
        }

        isInitialized = true;
        console.log('위치 추적기 초기화 완료 :', state);

        // 4. 설정이 완료된 후, start() 메소드로 추적을 명시적으로 시작합니다.
        if (shouldStart && !state.enabled) {
            await BackgroundGeolocation.start();
            console.log('백그라운드 위치 추적 시작!');
        }
    } catch (e) {
        console.error('위치 추적기 초기화 오류', e);
    }
}

/**
 * 백그라운드 위치 추적을 중지하는 함수
 */
export async function stopBackgroundTracking() {
    try {
        await BackgroundGeolocation.stop();
        console.log('백그라운드 위치 추적이 중지되었습니다.');
    } catch (e) {
        console.error('위치 추적기 중지 중 오류 발생:', e);
    }
}