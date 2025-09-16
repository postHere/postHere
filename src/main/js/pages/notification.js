// pages/notification.js
// 역할: 네이티브 환경에서 FCM 토큰 업로드를 보장(초기 1회)
// 규칙: 모든 코드는 initNotification() 내부에 작성

import {Capacitor} from '@capacitor/core';
import {PushNotifications} from '@capacitor/push-notifications';

export function initNotification() {

    //모든 코드는 이 안에 넣어야 합니다 예외는 없습니다

    (async () => {
        try {
            // 웹이 아닌 네이티브 환경에서만 동작
            if (!Capacitor.isNativePlatform()) return;

            // 권한
            const perm = await PushNotifications.requestPermissions();
            if (perm.receive !== 'granted') return;

            // 등록 (토큰 발급 트리거)
            await PushNotifications.register();

            // 토큰 수신 → 서버 업로드
            PushNotifications.addListener('registration', async ({value: token}) => {
                try {
                    await fetch('/api/push/token', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({
                            token,
                            platform: 'android',
                            app: 'post-here'
                        })
                    });
                } catch (e) {
                    console.error('FCM token upload failed', e);
                }
            });

            // 오류 로깅
            PushNotifications.addListener('registrationError', (err) => {
                console.error('FCM registration error', err);
            });

            // 포그라운드 수신 시 로그
            PushNotifications.addListener('pushNotificationReceived', (n) => {
                console.log('push foreground', n);
            });
        } catch (e) {
            console.error('initNotification failed', e);
        }
    })();
}
