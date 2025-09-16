// main/js/pages/push.js
// 네이티브 푸시(Firebase Cloud Messaging) 초기화

export function initPush() {

    // 모든 코드는 반드시 initPush 안에 작성합니다 (컨벤션 준수).

    import('@capacitor/push-notifications').then(async ({PushNotifications}) => {
        let __pushInitialized = false;
        if (__pushInitialized) return;
        __pushInitialized = true;

        try {
            // 권한 요청
            const perm = await PushNotifications.requestPermissions();
            if (!perm || perm.receive !== 'granted') {
                console.warn('[push] permission not granted');
                return;
            }

            // 디바이스 등록
            await PushNotifications.register();

            // 토큰 발급 시 서버 업로드
            PushNotifications.addListener('registration', async ({value: token}) => {
                try {
                    await fetch('/api/push/token', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({
                            token,
                            platform: 'android',
                            app: 'post-here',
                        }),
                    });
                    console.log('[push] token uploaded');
                } catch (e) {
                    console.error('[push] token upload failed', e);
                }
            });

            // 오류 처리
            PushNotifications.addListener('registrationError', (err) => {
                console.error('FCM registration error', err);
            });

            // 포그라운드 수신
            PushNotifications.addListener('pushNotificationReceived', (n) => {
                console.log('push foreground', n);
            });

        } catch (e) {
            console.error('[push] init error', e);
        }
    });

}
