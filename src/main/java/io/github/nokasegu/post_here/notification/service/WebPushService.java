package io.github.nokasegu.post_here.notification.service;

import com.google.gson.Gson;
import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.notification.repository.PushSubscriptionRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * [Flow (B) 연결 지점]
 * - FollowingService.follow(...) 성공
 * → NotificationService.createFollowAndPush(...)
 * → WebPushService.sendToUser(...) 에서 실제 Web Push 전송
 * <p>
 * [전제/의존]
 * - CryptoProviderConfig 등에서 애플리케이션 기동 시 BouncyCastle 등록 필요
 * - build.gradle: web-push(java), bcprov, bcpkix, gson 의존성 포함
 * - application-secret.yml: webpush.subject / webpush.publicKey / webpush.privateKey
 * <p>
 * [SW 연계]
 * - service-worker.js 'push' 핸들러가 수신 가능한 JSON 스키마를 준수해야 함
 * (예: { type, notificationId, actor{nickname,profilePhotoUrl}, text })
 */

/**
 * WebPushService
 * <p>
 * - 외부 라이브러리 nl.martijndwars.webpush.PushService 를 감싸는 어댑터 서비스
 * - VAPID 키 기반으로 Web Push 암호화/서명을 처리하고, 브라우저 endpoint로 알림 전송
 * <p>
 * 작동 원리:
 * 1. 사용자(UserInfoEntity)에 연결된 모든 PushSubscriptionEntity 를 DB에서 조회
 * 2. payload(Map)를 JSON 직렬화 후 바이트 배열로 변환
 * 3. PushService (외부 라이브러리)로 Notification 객체 생성 (endpoint, p256dh, auth, payload)
 * 4. send() 호출 → 브라우저 푸시 서버를 통해 클라이언트로 전달
 * <p>
 * 구현 포인트:
 * - buildClient() : VAPID subject/public/private 키로 PushService 초기화
 * - sendToUser() : 특정 유저에게 등록된 모든 endpoint로 반복 전송
 * - IOException, JoseException, ExecutionException 발생 시 로그만 기록
 * - InterruptedException 은 반드시 Thread.currentThread().interrupt() 로 복구 후 중단
 * <p>
 * 확장 포인트:
 * - 현재는 JSON 직렬화만 지원하지만, 향후 payload 구조 확장 가능
 * - 실패한 endpoint 를 DB에서 제거하는 로직 추가 가능
 * - 푸시 메시지 우선순위, TTL(Time-To-Live) 같은 설정값 확장 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    // 순환 의존 방지: NotificationService 를 직접 주입하지 않음
    // private final NotificationService notificationService;

    private final WebPushProperties props;               // VAPID 키/subject 등 구성 정보
    private final PushSubscriptionRepository pushRepo;   // 사용자 구독 정보 저장소
    private final Gson gson = new Gson();                // payload 직렬화용

    /**
     * PushService 클라이언트 생성 (외부 라이브러리)
     * - VAPID subject, public/private 키를 설정
     */
    private PushService buildClient() throws GeneralSecurityException {
        PushService svc = new PushService();
        svc.setSubject(props.getSubject());
        svc.setPublicKey(props.getPublicKey());
        svc.setPrivateKey(props.getPrivateKey());
        return svc;
    }

    /**
     * 특정 사용자에게 Web Push 알림 전송
     *
     * @param target  알림 받을 사용자
     * @param payload JSON으로 직렬화될 데이터(Map<String, Object>)
     */
    public void sendToUser(UserInfoEntity target, Map<String, Object> payload) {
        List<PushSubscriptionEntity> subs = pushRepo.findAllByUser(target);
        if (subs.isEmpty()) return;

        final byte[] data = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        try {
            PushService client = buildClient();

            for (PushSubscriptionEntity s : subs) {
                try {
                    // 브라우저 endpoint + 키 정보 + 데이터로 Notification 객체 생성
                    Notification n = new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), data);
                    client.send(n); // 실제 전송
                } catch (IOException | JoseException | ExecutionException e) {
                    log.warn("WebPush send fail endpoint={}", s.getEndpoint(), e);
                } catch (InterruptedException e) {
                    // 인터럽트 신호 복구 필수
                    Thread.currentThread().interrupt();
                    log.warn("WebPush send interrupted endpoint={}", s.getEndpoint(), e);
                    return; // 현재 스레드가 인터럽트된 상태이므로 루프 중단
                }
            }
        } catch (GeneralSecurityException e) {
            log.error("WebPush init error", e);
        }
    }
}
