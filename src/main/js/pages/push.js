// src/main/js/pages/push.js
// 네이티브 푸시(Firebase Cloud Messaging) + 포어그라운드 배너(LocalNotifications)

export function initPush() {
    import('@capacitor/core').then(async ({Capacitor}) => {
        if (!Capacitor.isNativePlatform()) return;

        const {PushNotifications} = await import('@capacitor/push-notifications');

        // LocalNotifications 플러그인 준비
        let LocalNotifications = null;
        let hasLocal = false;
        try {
            const mod = await import('@capacitor/local-notifications');
            LocalNotifications = mod.LocalNotifications;
            hasLocal = Capacitor.isPluginAvailable('LocalNotifications');
            console.log('[push] LocalNotifications available:', hasLocal);
        } catch (e) {
            console.warn('[push] LocalNotifications import failed:', e);
        }

        let __pushInitialized = false;
        if (__pushInitialized) return;
        __pushInitialized = true;

        try {
            // 토큰 업로드
            PushNotifications.addListener('registration', async ({value: token}) => {
                try {
                    console.log('[push] token head=', token?.slice?.(0, 12));
                    await fetch('/api/push/token', {
                        method: 'POST',
                        credentials: 'include',
                        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
                        body: JSON.stringify({token, platform: 'android', app: 'post-here'}),
                    });
                    console.log('[push] token uploaded');
                } catch (e) {
                    console.error('[push] token upload failed', e);
                }
            });

            PushNotifications.addListener('registrationError', (err) => {
                console.error('FCM registration error', err);
            });

            // ★ 포어그라운드 수신 시 → 로컬 배너 띄우기
            PushNotifications.addListener('pushNotificationReceived', async (n) => {
                try {
                    console.log('[push] foreground payload =', JSON.stringify(n));

                    if (!hasLocal || !LocalNotifications) {
                        console.warn('[push] LocalNotifications not available → 배너 생략');
                        return;
                    }

                    // 권한 확인/요청 (Android 13+)
                    const permBefore = await LocalNotifications.checkPermissions();
                    if (permBefore.display !== 'granted') {
                        const req = await LocalNotifications.requestPermissions();
                        if (req.display !== 'granted') {
                            console.warn('[push] LocalNotifications permission denied');
                            return;
                        }
                    }

                    // 채널 생성/보장 (중요도 HIGH)
                    await LocalNotifications.createChannel?.({
                        id: 'default',
                        name: 'General',
                        description: 'General notifications',
                        importance: 5,  // IMPORTANCE_HIGH
                        visibility: 1,  // VISIBILITY_PUBLIC
                        sound: 'default',
                    });

                    // 타이틀/본문 안전 추출
                    const title =
                        n?.notification?.title ??
                        n?.title ??
                        n?.data?.title ??
                        (n?.data?.actorNickname ? `${n.data.actorNickname}님이 알림을 보냈습니다` : '새 알림');

                    const body =
                        n?.notification?.body ??
                        n?.body ??
                        n?.data?.body ??
                        n?.data?.message ??
                        (n?.data?.type === 'FOLLOW' ? '새 팔로우' : '메시지가 도착했습니다');

                    // 즉시 스케줄 (약간의 딜레이를 주는 것이 안전)
                    const at = new Date(Date.now() + 600);
                    const id = Date.now() % 100000;

                    const res = await LocalNotifications.schedule({
                        notifications: [
                            {
                                id,
                                title,
                                body,
                                schedule: {at},
                                channelId: 'default',
                                smallIcon: 'ic_launcher', // 앱 기본 아이콘
                                extra: n?.data ?? {},
                            },
                        ],
                    });
                    console.log('[push] LocalNotifications.schedule result:', res);
                } catch (e) {
                    console.error('[push] foreground handler failed', e);
                }
            });

            // 권한/등록
            const perm = await PushNotifications.requestPermissions();
            if (!perm || perm.receive !== 'granted') {
                console.warn('[push] permission not granted');
                return;
            }
            await PushNotifications.register();

            // 콘솔 테스트용 헬퍼
            if (hasLocal && LocalNotifications) {
                window.__localTest = async () => {
                    const r = await LocalNotifications.requestPermissions();
                    if (r.display !== 'granted') return;
                    await LocalNotifications.createChannel?.({
                        id: 'default', name: 'General', importance: 5, visibility: 1,
                    });
                    await LocalNotifications.schedule({
                        notifications: [{
                            id: Math.floor(Math.random() * 100000),
                            title: '로컬 테스트', body: 'LocalNotifications 테스트',
                            schedule: {at: new Date(Date.now() + 800)}, channelId: 'default',
                        }],
                    });
                    console.log('__localTest done');
                };
                console.log('[push] window.__localTest 준비됨');
            }
        } catch (e) {
            console.error('[push] init error', e);
        }
    });
}
