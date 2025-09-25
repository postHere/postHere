package io.github.nokasegu.post_here.notification.service;

import com.google.gson.Gson;
import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.notification.repository.PushSubscriptionRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * WebPushService
 * - PushSubscriptionRepository 에 저장된 endpoint/p256dh/auth 값을 불러와
 * VAPID 키로 서명한 WebPush Notification을 전송한다.
 * - 실패 시(404/410 Gone) 구독 정보를 DB에서 삭제한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {
    private final PushSubscriptionRepository pushRepo;
    private final WebPushProperties props;

    public void sendToUser(UserInfoEntity user, Map<String, Object> payload) {
        List<PushSubscriptionEntity> subs = pushRepo.findAllByUser(user);
        if (subs.isEmpty()) {
            log.info("[WebPush] no subscriptions for user {}", user.getId());
            return;
        }

        int ok = 0, gone = 0, other = 0;
        Gson gson = new Gson();

        for (PushSubscriptionEntity sub : subs) {
            try {
                PushService service = new PushService();
                service.setPublicKey(props.getPublicKey());
                service.setPrivateKey(props.getPrivateKey());
                service.setSubject(props.getSubject());

                byte[] body = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

                Notification n = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        body
                );

                HttpResponse res = service.send(n);
                int status = res.getStatusLine().getStatusCode();

                if (status == 201) {
                    ok++;
                } else if (status == 404 || status == 410) {
                    pushRepo.delete(sub);
                    gone++;
                } else {
                    other++;
                    log.warn("[WebPush] non-201 status={} endpointTail={}", status,
                            sub.getEndpoint().substring(Math.max(0, sub.getEndpoint().length() - 12)));
                }
            } catch (Exception ex) {
                log.warn("[WebPush] send failed endpointTail={}",
                        sub.getEndpoint().substring(Math.max(0, sub.getEndpoint().length() - 12)), ex);
                other++;
            }
        }
        log.info("[WebPush] done userId={} result: ok={}, gone={}, other={}",
                user.getId(), ok, gone, other);
    }
}
